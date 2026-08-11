package com.financematch.report.service;

import com.financematch.report.dto.reason.TaxAccountInput;
import com.financematch.report.dto.reason.TaxSavingProfile;
import com.financematch.report.dto.reason.TaxStrategyReasonInput;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 절세 축 reason 문구를 결정론적 규칙으로 생성한다. 팀 확정(GAP-07·GAP-08·GAP-11) 반영:
 *
 * <ul>
 *   <li>GAP-07: 세 계좌 다 미개설이면 개별 나열 대신 뭉뚱그린 문구
 *   <li>GAP-08: 이름은 성을 뗀 축약형으로만 노출 ({@link KoreanNameFormatter})
 *   <li>GAP-11: 두 사람 다 언급할 때는 이름 가나다순 — 정렬은 반드시 원래 전체 이름 기준으로 한다
 *       (축약형끼리 비교하면 가나다 순서가 뒤바뀔 수 있다)
 * </ul>
 */
@Service
public class TaxStrategyReasonService {

    private static final List<String> ACCOUNT_NAMES = List.of("ISA", "IRP", "연금저축");

    public String generate(TaxStrategyReasonInput input) {
        return fallback(input);
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

        boolean firstUnopened = hasUnopenedAccount(firstProfile);
        boolean secondUnopened = hasUnopenedAccount(secondProfile);
        String recommendationTab = "추천탭에서 상품들을 만나보세요.";

        // 미개설 계좌가 있는 사람이 둘 다면 한 사람 것으로 특정할 수 없으니 맨 뒤에서 한 번만 안내하고,
        // 한 명뿐이면 그 사람 설명 바로 뒤에 붙인다 — 등장 순서가 앞이든 뒤든 항상 해당 설명 옆에 온다.
        if (firstUnopened && secondUnopened) {
            return firstDesc + " " + secondDesc + " " + recommendationTab;
        }
        if (firstUnopened) {
            return secondDesc != null
                    ? firstDesc + " " + recommendationTab + " " + secondDesc
                    : firstDesc + " " + recommendationTab;
        }
        if (secondUnopened) {
            return firstDesc != null
                    ? firstDesc + " " + secondDesc + " " + recommendationTab
                    : secondDesc + " " + recommendationTab;
        }
        return firstDesc != null && secondDesc != null ? firstDesc + " " + secondDesc : (firstDesc != null ? firstDesc : secondDesc);
    }

    private boolean hasUnopenedAccount(TaxSavingProfile profile) {
        return !profile.getIsa().isOpened()
                || !profile.getIrp().isOpened()
                || !profile.getPension().isOpened();
    }

    /** 문제 없으면 null(언급 안 함). 문제 있으면 그 사람에 대한 문장 하나. name 은 이미 축약형이어야 한다. */
    private String describePerson(String name, TaxSavingProfile profile) {
        List<String> unopened = unopenedAccountNames(profile);

        if (unopened.size() == ACCOUNT_NAMES.size()) {
            // GAP-07: 뭉뚱그린 문구
            return name + "님은 " + String.join("·", ACCOUNT_NAMES) + " 모두 개설 안 하셨어요.";
        }
        if (!unopened.isEmpty()) {
            return name + "님은 " + String.join("·", unopened) + " 계좌를 개설하지 않았어요.";
        }

        List<String> underLimitClauses = new ArrayList<>();
        addUnderLimitClause(underLimitClauses, "ISA", profile.getIsa());
        addUnderLimitClause(underLimitClauses, "IRP", profile.getIrp());
        addUnderLimitClause(underLimitClauses, "연금저축", profile.getPension());

        if (!underLimitClauses.isEmpty()) {
            // "한도"만 쓰면 계좌 잔액(예: irp_balance) 한도로 오해할 수 있다 — 실제로는 연간 납입
            // 한도(irp_annual_payment 같은 올해 납입액과 비교)라 "올해"를 명시한다.
            return name + "님은 올해 " + String.join("·", underLimitClauses) + "을 다 채우지 않았어요. "
                    + "더 채우고 세제 혜택 받으세요.";
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
            clauses.add(accountName + " 한도 " + WonAmountFormatter.format(account.getAnnualLimit()));
        }
    }
}
