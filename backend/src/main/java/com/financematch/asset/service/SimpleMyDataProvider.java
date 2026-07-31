package com.financematch.asset.service;

import com.financematch.asset.domain.AssetCategory;
import com.financematch.asset.domain.MyDataAsset;
import com.financematch.asset.domain.MyDataLoan;
import com.financematch.asset.domain.MyDataPensionIsa;
import com.financematch.asset.domain.MyDataSnapshot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 목 서버 도입 전 사용하는 예비부부용 개인 마이데이터 시나리오.
 *
 * <p>회원가입 직후에는 아직 커플 관계가 없으므로 회원 ID로 개인 시나리오를 결정한다. 홀수 회원은
 * 부채가 없는 A, 짝수 회원은 전세자금대출이 있는 B다. 목 서버 도입 후에는 이 임시 구분 없이 서버가
 * 회원별 데이터를 반환한다.
 */
@Component
public class SimpleMyDataProvider implements MyDataProvider {

    @Override
    public MyDataSnapshot fetch(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID가 필요합니다.");
        }

        return memberId % 2 == 1
                ? debtFreeSnapshot()
                : housingLoanSnapshot();
    }

    private MyDataSnapshot debtFreeSnapshot() {
        List<MyDataAsset> assets =
                List.of(
                        asset(
                                "KB국민은행",
                                "KB국민ONE통장",
                                AssetCategory.BANK_CHECKING,
                                12_630_000L),
                        asset(
                                "KB국민은행",
                                "KB Star 정기예금",
                                AssetCategory.BANK_SAVINGS,
                                50_520_000L),
                        asset(
                                "KB증권",
                                "KB증권 종합위탁",
                                AssetCategory.SECURITIES,
                                9_050_000L),
                        asset(
                                "KB증권",
                                "KB 연금저축펀드",
                                AssetCategory.SECURITIES,
                                6_000_000L),
                        asset(
                                "KB증권",
                                "KB 중개형 ISA",
                                AssetCategory.SECURITIES,
                                6_000_000L));

        MyDataPensionIsa pensionIsa =
                new MyDataPensionIsa(
                        true,
                        false,
                        false,
                        true,
                        BigDecimal.valueOf(6_000_000L),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(4_800_000L),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(6_000_000L),
                        "ELIGIBLE",
                        "ELIGIBLE");

        return new MyDataSnapshot(
                assets,
                List.of(),
                pensionIsa,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private MyDataSnapshot housingLoanSnapshot() {
        List<MyDataAsset> assets =
                List.of(
                        asset(
                                "토스뱅크",
                                "토스뱅크 통장",
                                AssetCategory.BANK_CHECKING,
                                8_400_000L),
                        asset(
                                "KB국민은행",
                                "주택청약종합저축·정기예금",
                                AssetCategory.BANK_SAVINGS,
                                30_600_000L),
                        asset(
                                "KB증권",
                                "인덱스펀드",
                                AssetCategory.SECURITIES,
                                20_000_000L),
                        asset(
                                "KB증권",
                                "KB 개인형IRP",
                                AssetCategory.SECURITIES,
                                4_000_000L));

        MyDataPensionIsa pensionIsa =
                new MyDataPensionIsa(
                        false,
                        true,
                        false,
                        false,
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(4_000_000L),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(3_000_000L),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "ELIGIBLE",
                        "ELIGIBLE");

        return new MyDataSnapshot(
                assets,
                List.of(
                        new MyDataLoan(
                                "KB국민은행",
                                "KB 전세금안심대출",
                                BigDecimal.valueOf(35_000_000L),
                                BigDecimal.valueOf(3_600_000L),
                                new BigDecimal("4.15"),
                                false)),
                pensionIsa,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private MyDataAsset asset(
            String institutionName,
            String productName,
            AssetCategory category,
            long balance) {
        return new MyDataAsset(
                institutionName,
                productName,
                category,
                BigDecimal.valueOf(balance));
    }
}
