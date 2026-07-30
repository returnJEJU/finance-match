package com.financematch.report.service;

import com.financematch.report.dto.reason.TaxAccountInput;
import com.financematch.report.dto.reason.TaxSavingProfile;
import com.financematch.report.dto.reason.TaxStrategyReasonInput;
import com.financematch.report.llm.ReasonRuleValidator;
import com.financematch.report.llm.ReportLlmClient;
import com.financematch.report.llm.ReportPromptBuilder;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 절세 축 reason 문구를 LLM으로 생성한다. 팀 확정(GAP-07·GAP-08·GAP-09·GAP-11) 반영:
 *
 * <ul>
 *   <li>GAP-07: 세 계좌 다 미개설이면 개별 나열 대신 뭉뚱그린 문구
 *   <li>GAP-08: 이름은 성을 뗀 축약형으로만 노출 ({@link KoreanNameFormatter})
 *   <li>GAP-09: 이 축만 길이 상한을 {@value ReportPromptBuilder#TAX_STRATEGY_MAX_LENGTH}자로 확장
 *   <li>GAP-11: 두 사람 다 언급할 때는 이름 가나다순 — 정렬은 반드시 원래 전체 이름 기준으로 한다
 *       (축약형끼리 비교하면 가나다 순서가 뒤바뀔 수 있다)
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxStrategyReasonService {

    private static final int MAX_ATTEMPTS = 3;
    private static final List<String> ACCOUNT_NAMES = List.of("ISA", "IRP", "연금저축");

    private final ReportLlmClient llmClient;
    private final ReportPromptBuilder promptBuilder;
    private final ReasonRuleValidator ruleValidator;

    public String generate(TaxStrategyReasonInput input) {
        String prompt = promptBuilder.buildTaxStrategyPrompt(withAbbreviatedNames(input));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String candidate = llmClient.generateReason(prompt);
            if (isValid(candidate, input)) {
                return candidate;
            }
            log.warn("절세 축 reason 검증 실패(시도 {}/{}): {}", attempt, MAX_ATTEMPTS, candidate);
        }

        log.warn("절세 축 reason LLM 생성 {}회 모두 실패 — 결정론적 문구로 폴백", MAX_ATTEMPTS);
        return fallback(input);
    }

    private TaxStrategyReasonInput withAbbreviatedNames(TaxStrategyReasonInput input) {
        return new TaxStrategyReasonInput(
                KoreanNameFormatter.abbreviate(input.getMeName()),
                KoreanNameFormatter.abbreviate(input.getPartnerName()),
                input.getMe(),
                input.getPartner());
    }

    private boolean isValid(String reason, TaxStrategyReasonInput input) {
        if (!ruleValidator
                .validateCommon(reason, ReportPromptBuilder.TAX_STRATEGY_MAX_LENGTH)
                .valid()) {
            return false;
        }

        String meDisplay = KoreanNameFormatter.abbreviate(input.getMeName());
        String partnerDisplay = KoreanNameFormatter.abbreviate(input.getPartnerName());

        boolean meHasIssue = describePerson(meDisplay, input.getMe()) != null;
        boolean partnerHasIssue = describePerson(partnerDisplay, input.getPartner()) != null;

        boolean meMentioned = reason.contains(meDisplay);
        boolean partnerMentioned = reason.contains(partnerDisplay);
        if (meMentioned != meHasIssue || partnerMentioned != partnerHasIssue) {
            return false;
        }

        // GAP-11: 둘 다 언급되면 가나다순으로 등장해야 한다. 정렬은 원래 전체 이름 기준.
        if (meMentioned && partnerMentioned) {
            boolean meFirst = input.getMeName().compareTo(input.getPartnerName()) <= 0;
            String expectedFirst = meFirst ? meDisplay : partnerDisplay;
            String expectedSecond = meFirst ? partnerDisplay : meDisplay;
            if (reason.indexOf(expectedFirst) > reason.indexOf(expectedSecond)) {
                return false;
            }
        }

        // 미개설 계좌가 하나라도 있으면 추천탭 언급이 필수다.
        boolean anyUnopened =
                hasUnopenedAccount(input.getMe()) || hasUnopenedAccount(input.getPartner());
        return !anyUnopened || reason.contains("추천탭");
    }

    private boolean hasUnopenedAccount(TaxSavingProfile profile) {
        return !profile.getIsa().isOpened()
                || !profile.getIrp().isOpened()
                || !profile.getPension().isOpened();
    }

    String fallback(TaxStrategyReasonInput input) {
        // GAP-11: 등장 순서 정렬은 원래 전체 이름 기준으로 하고, 표시는 축약형으로 한다.
        boolean meFirst = input.getMeName().compareTo(input.getPartnerName()) <= 0;
        String firstDisplayName =
                KoreanNameFormatter.abbreviate(meFirst ? input.getMeName() : input.getPartnerName());
        TaxSavingProfile firstProfile = meFirst ? input.getMe() : input.getPartner();
        String secondDisplayName =
                KoreanNameFormatter.abbreviate(meFirst ? input.getPartnerName() : input.getMeName());
        TaxSavingProfile secondProfile = meFirst ? input.getPartner() : input.getMe();

        String firstDesc = describePerson(firstDisplayName, firstProfile);
        String secondDesc = describePerson(secondDisplayName, secondProfile);

        if (firstDesc == null && secondDesc == null) {
            return "ISA·IRP·연금저축을 모두 개설하고 한도도 다 채워 절세 혜택을 제대로 누리고 계시네요!";
        }
        if (firstDesc != null && secondDesc != null) {
            return firstDesc + " " + secondDesc;
        }
        return firstDesc != null ? firstDesc : secondDesc;
    }

    /** 문제 없으면 null(언급 안 함). 문제 있으면 그 사람에 대한 문장 하나. name 은 이미 축약형이어야 한다. */
    private String describePerson(String name, TaxSavingProfile profile) {
        List<String> unopened = unopenedAccountNames(profile);

        if (unopened.size() == ACCOUNT_NAMES.size()) {
            // GAP-07: 뭉뚱그린 문구
            return name + "님은 " + String.join("·", ACCOUNT_NAMES) + " 모두 개설 안 하셨어요. "
                    + "추천탭에서 상품들을 만나보세요.";
        }
        if (!unopened.isEmpty()) {
            return name
                    + "님은 "
                    + String.join("·", unopened)
                    + " 계좌를 개설하지 않았어요. 추천탭에서 상품들을 만나보세요.";
        }

        List<String> underLimitClauses = new ArrayList<>();
        addUnderLimitClause(underLimitClauses, "ISA", profile.getIsa());
        addUnderLimitClause(underLimitClauses, "IRP", profile.getIrp());
        addUnderLimitClause(underLimitClauses, "연금저축", profile.getPension());

        if (!underLimitClauses.isEmpty()) {
            return name + "님은 " + String.join(", ", underLimitClauses) + " 더 채우면 혜택을 더 받을 수 있어요.";
        }

        return null;
    }

    private List<String> unopenedAccountNames(TaxSavingProfile profile) {
        List<String> unopened = new ArrayList<>();
        if (!profile.getIsa().isOpened()) {
            unopened.add("ISA");
        }
        if (!profile.getIrp().isOpened()) {
            unopened.add("IRP");
        }
        if (!profile.getPension().isOpened()) {
            unopened.add("연금저축");
        }
        return unopened;
    }

    private void addUnderLimitClause(List<String> clauses, String accountName, TaxAccountInput account) {
        if (account.isUnderLimit()) {
            clauses.add(
                    accountName
                            + " 한도 "
                            + WonAmountFormatter.format(account.getAnnualLimit())
                            + " 중 "
                            + WonAmountFormatter.format(account.getMaxBenefit())
                            + " 혜택 가능");
        }
    }
}
