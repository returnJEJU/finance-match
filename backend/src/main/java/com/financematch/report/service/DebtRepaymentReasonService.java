package com.financematch.report.service;

import com.financematch.report.dto.reason.DebtRepaymentReasonInput;
import com.financematch.report.llm.ReasonRuleValidator;
import com.financematch.report.llm.ReportLlmClient;
import com.financematch.report.llm.ReportPromptBuilder;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 부채 축 reason 문구를 LLM으로 생성한다.
 *
 * <p>규칙 위반(뷰어 종속 표현, 이름 오선택 등) 시 최대 {@value #MAX_ATTEMPTS}회 재시도하고, 그래도
 * 실패하면 {@link #fallback} 의 결정론적 문구로 대체한다 — 금융 서비스 특성상 검증 안 된 문장을
 * 그대로 노출하지 않는다.
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
        String prompt = promptBuilder.buildDebtRepaymentPrompt(input);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String candidate = llmClient.generateReason(prompt);
            if (isValid(candidate, input)) {
                return candidate;
            }
            log.warn("부채 축 reason 검증 실패(시도 {}/{}): {}", attempt, MAX_ATTEMPTS, candidate);
        }

        log.warn("부채 축 reason LLM 생성 {}회 모두 실패 — 결정론적 문구로 폴백", MAX_ATTEMPTS);
        return fallback(input);
    }

    private boolean isValid(String reason, DebtRepaymentReasonInput input) {
        if (!ruleValidator.validateCommon(reason).valid()) {
            return false;
        }
        return matchesExpectedNameUsage(reason, input);
    }

    // R4: 위험도 차이가 있을 때만 "더 위험한 쪽" 이름 하나만 등장해야 하고, 그 외 케이스는 이름이 아예 없어야 한다.
    private boolean matchesExpectedNameUsage(String reason, DebtRepaymentReasonInput input) {
        boolean meNamed = reason.contains(input.getMeName());
        boolean partnerNamed = reason.contains(input.getPartnerName());

        String expectedHigherRiskName = higherRiskName(input);
        if (expectedHigherRiskName == null) {
            return !meNamed && !partnerNamed;
        }

        boolean expectMeNamed = expectedHigherRiskName.equals(input.getMeName());
        return meNamed == expectMeNamed && partnerNamed != expectMeNamed;
    }

    /** 둘 다 부채 있고 위험도가 다를 때만 이름을 반환한다. 그 외엔 null. */
    private String higherRiskName(DebtRepaymentReasonInput input) {
        if (!input.isMeHasDebt() || !input.isPartnerHasDebt()) {
            return null;
        }
        if (Objects.equals(input.getMeRiskLevel(), input.getPartnerRiskLevel())) {
            return null;
        }
        return input.getMeRiskLevel() > input.getPartnerRiskLevel()
                ? input.getMeName()
                : input.getPartnerName();
    }

    String fallback(DebtRepaymentReasonInput input) {
        if (!input.isMeHasDebt() && !input.isPartnerHasDebt()) {
            return "두 분 모두 부채가 없어 높은 점수가 나왔어요.";
        }
        if (input.isMeHasDebt() && input.isPartnerHasDebt()) {
            String higherRiskName = higherRiskName(input);
            if (higherRiskName == null) {
                return "두 분 모두 부채 위험도가 높아요.";
            }
            return "두 분 모두 부채가 있어요. 하지만 부채 위험도는 " + higherRiskName + "님이 더 높아요.";
        }
        return "두 분 중 부채가 있는 분이 있어요.";
    }
}
