package com.financematch.report.service;

import com.financematch.report.dto.reason.DebtRepaymentReasonInput;
import com.financematch.report.llm.ReasonRuleValidator;
import com.financematch.report.llm.ReportLlmClient;
import com.financematch.report.llm.ReportPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 부채 축 reason 문구를 LLM으로 생성한다.
 *
 * <p>GAP-02 팀 확정: "위험도 차이"는 임계값 없이 {@code meScore}/{@code partnerScore}(둘 다
 * 부채있음 케이스에서)를 직접 비교한다 — 조금이라도 낮은 쪽(=더 위험한 쪽)을 지목하고, 정확히 같을
 * 때만(사실상 거의 없음) 이름 없이 처리한다.
 *
 * <p>규칙 위반 시 최대 {@value #MAX_ATTEMPTS}회 재시도하고, 그래도 실패하면 {@link #fallback}의
 * 결정론적 문구로 대체한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DebtRepaymentReasonService {

    private static final int MAX_ATTEMPTS = 3;

    private final ReportLlmClient llmClient;
    private final ReportPromptBuilder promptBuilder;
    private final ReasonRuleValidator ruleValidator;

    public String generate(DebtRepaymentReasonInput input) {
        String prompt = promptBuilder.buildDebtRepaymentPrompt(withAbbreviatedNames(input));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String candidate;
            try {
                candidate = llmClient.generateReason(prompt);
            } catch (ReportLlmClient.LlmCallException e) {
                log.warn("부채 축 reason LLM 호출 실패(시도 {}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage());
                continue;
            }
            if (isValid(candidate, input)) {
                return candidate;
            }
            log.warn("부채 축 reason 검증 실패(시도 {}/{}): {}", attempt, MAX_ATTEMPTS, candidate);
        }

        log.warn("부채 축 reason LLM 생성 {}회 모두 실패 — 결정론적 문구로 폴백", MAX_ATTEMPTS);
        return fallback(input);
    }

    private DebtRepaymentReasonInput withAbbreviatedNames(DebtRepaymentReasonInput input) {
        return new DebtRepaymentReasonInput(
                KoreanNameFormatter.abbreviate(input.getMeName()),
                KoreanNameFormatter.abbreviate(input.getPartnerName()),
                input.isMeHasDebt(),
                input.isPartnerHasDebt(),
                input.getMeScore(),
                input.getPartnerScore());
    }

    private boolean isValid(String reason, DebtRepaymentReasonInput input) {
        if (!ruleValidator.validateCommon(reason).valid()) {
            return false;
        }

        String meDisplay = KoreanNameFormatter.abbreviate(input.getMeName());
        String partnerDisplay = KoreanNameFormatter.abbreviate(input.getPartnerName());
        boolean meMentioned = reason.contains(meDisplay);
        boolean partnerMentioned = reason.contains(partnerDisplay);

        if (!input.isMeHasDebt() && !input.isPartnerHasDebt()) {
            return !meMentioned && !partnerMentioned;
        }

        int scoreCompare = input.getMeScore().compareTo(input.getPartnerScore());
        if (input.isMeHasDebt() && input.isPartnerHasDebt() && scoreCompare == 0) {
            return !meMentioned && !partnerMentioned;
        }

        // score 가 더 낮은 쪽(=더 큰 영향을 준 쪽)만 언급돼야 한다.
        boolean expectMeMentioned = scoreCompare < 0;
        return meMentioned == expectMeMentioned && partnerMentioned != expectMeMentioned;
    }

    String fallback(DebtRepaymentReasonInput input) {
        boolean meHasDebt = input.isMeHasDebt();
        boolean partnerHasDebt = input.isPartnerHasDebt();

        if (!meHasDebt && !partnerHasDebt) {
            return "두 분 모두 부채가 없어 높은 점수가 나왔어요.";
        }

        int scoreCompare = input.getMeScore().compareTo(input.getPartnerScore());

        if (meHasDebt && partnerHasDebt) {
            if (scoreCompare == 0) {
                return "두 분 모두 부채 위험도가 높아요.";
            }
            String riskierName =
                    KoreanNameFormatter.abbreviate(
                            scoreCompare < 0 ? input.getMeName() : input.getPartnerName());
            return "두 분 모두 부채가 있어요. 하지만 부채 위험도는 " + riskierName + "님이 더 높아요.";
        }

        // 정확히 한 명만 부채 있음 — score 가 더 낮은 쪽(=부채 있는 쪽)을 지목.
        String affectedName =
                KoreanNameFormatter.abbreviate(
                        scoreCompare < 0 ? input.getMeName() : input.getPartnerName());
        return "부채 점수 감점에 " + affectedName + "님이 더 큰 영향을 끼쳤어요.";
    }
}
