package com.financematch.asset.service;

import com.financematch.asset.domain.AssetCategory;
import com.financematch.asset.domain.MyDataAsset;
import com.financematch.asset.domain.MyDataLoan;
import com.financematch.asset.domain.MyDataPensionIsa;
import com.financematch.asset.domain.MyDataSnapshot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 목 서버 도입 전 사용하는 데모 회원별 마이데이터 시나리오.
 *
 * <p>회원과 시나리오는 {@link SimpleMyDataProvider}가 이메일로 연결한다. 자산은 사람에게 붙어 있으므로
 * 커플 조합을 바꿔도 각자의 자산은 그대로 따라간다.
 *
 * <p>{@link #DEMO_INVITER}·{@link #DEMO_INVITEE}는 기존 데모 커플(김하나·이두리)이다. 두 시나리오의
 * 합계는 {@code R__02_seed_demo.sql}의 {@code financial_summary}·{@code pension_isa_account} 값과
 * 일치해야 한다 — 어긋나면 자산 조회 화면의 총액과 항목별 합계가 달라지고, 자산 갱신 한 번으로 데모
 * 궁합 점수(73.15)의 전제가 깨진다.
 *
 * <p>목 서버가 준비되면 이 파일과 {@link SimpleMyDataProvider}를 함께 지우고 {@link MyDataProvider}의
 * HTTP 구현으로 교체한다.
 */
public enum MyDataScenario {

    /** 김하나 · 안정추구 · 전세대출 2,000만. 기존 데모 시드와 값이 일치한다. */
    DEMO_INVITER {
        @Override
        MyDataSnapshot build(LocalDateTime fetchedAt) {
            return new MyDataSnapshot(
                    List.of(
                            checking("KB국민은행", "KB국민ONE통장", 4_000_000L),
                            savings("KB국민은행", "KB Star 정기예금", 60_000_000L),
                            securities("KB증권", "KB 연금저축펀드", 5_000_000L),
                            securities("KB증권", "KB 중개형 ISA", 21_000_000L)),
                    List.of(loan("KB국민은행", "KB 전세금안심대출", 20_000_000L, 6_000_000L, "4.50")),
                    accounts(
                            true, false, true,
                            5_000_000L, 0L,
                            6_000_000L, 0L, 10_000_000L),
                    fetchedAt);
        }
    },

    /** 이두리 · 적극투자 · 신용대출 4,000만. 기존 데모 시드와 값이 일치한다. */
    DEMO_INVITEE {
        @Override
        MyDataSnapshot build(LocalDateTime fetchedAt) {
            return new MyDataSnapshot(
                    List.of(
                            checking("토스뱅크", "토스뱅크 통장", 6_000_000L),
                            savings("KB국민은행", "KB Star 정기예금", 40_000_000L),
                            securities("KB증권", "인덱스펀드", 60_000_000L),
                            securities("KB증권", "해외주식 위탁계좌", 39_000_000L),
                            securities("KB증권", "KB 연금저축펀드", 20_000_000L),
                            securities("KB증권", "KB 개인형IRP", 15_000_000L)),
                    List.of(loan("KB국민은행", "KB 직장인든든 신용대출", 40_000_000L, 10_000_000L, "5.20")),
                    accounts(
                            true, true, true,
                            20_000_000L, 15_000_000L,
                            6_000_000L, 3_000_000L, 20_000_000L),
                    fetchedAt);
        }
    },

    /** 김보람 · 모으는 사람 · 자산 1.2억 · 전세대출 6,000만 · ISA 미보유. */
    SAVER {
        @Override
        MyDataSnapshot build(LocalDateTime fetchedAt) {
            return new MyDataSnapshot(
                    List.of(
                            checking("KB국민은행", "KB국민ONE통장", 15_000_000L),
                            savings("KB국민은행", "KB Star 정기예금", 70_000_000L),
                            savings("KB국민은행", "주택청약종합저축", 12_000_000L),
                            securities("KB증권", "KB증권 종합위탁", 8_000_000L),
                            securities("KB증권", "KB 연금저축펀드", 15_000_000L)),
                    List.of(loan("KB국민은행", "KB 전세금안심대출", 60_000_000L, 4_800_000L, "3.80")),
                    accounts(
                            true, false, false,
                            15_000_000L, 0L,
                            3_000_000L, 0L, 0L),
                    fetchedAt);
        }
    },

    /** 박준서 · 굴리는 사람 · 자산 1.23억 · 신용대출 2,000만 · 연금저축 미보유. */
    INVESTOR {
        @Override
        MyDataSnapshot build(LocalDateTime fetchedAt) {
            return new MyDataSnapshot(
                    List.of(
                            checking("토스뱅크", "토스뱅크 통장", 8_000_000L),
                            savings("KB국민은행", "KB e-plus 정기예금", 20_000_000L),
                            securities("KB증권", "인덱스펀드", 60_000_000L),
                            securities("KB증권", "해외주식 위탁계좌", 25_000_000L),
                            securities("KB증권", "KB 개인형IRP", 10_000_000L)),
                    List.of(loan("KB국민은행", "KB 직장인든든 신용대출", 20_000_000L, 6_000_000L, "5.90")),
                    accounts(
                            false, true, true,
                            0L, 10_000_000L,
                            0L, 3_000_000L, 5_000_000L),
                    fetchedAt);
        }
    },

    /** 최유진 · 전세 신혼 · 자산 350만 · 전세대출 1.2억 · 절세계좌 전무. */
    NEWLYWED {
        @Override
        MyDataSnapshot build(LocalDateTime fetchedAt) {
            return new MyDataSnapshot(
                    List.of(
                            checking("KB국민은행", "KB국민ONE통장", 1_200_000L),
                            savings("KB국민은행", "KB나만의 적금", 2_000_000L),
                            securities("KB증권", "KB증권 종합위탁", 300_000L)),
                    List.of(loan("KB국민은행", "KB 전세금안심대출", 120_000_000L, 9_600_000L, "4.60")),
                    noAccounts(),
                    fetchedAt);
        }
    },

    /** 정민호 · 대출 2건(가중평균 금리 7.87%) · 자산 240만 · 절세계좌 전무. */
    RENTER {
        @Override
        MyDataSnapshot build(LocalDateTime fetchedAt) {
            return new MyDataSnapshot(
                    List.of(
                            checking("카카오뱅크", "카카오뱅크 입출금통장", 900_000L),
                            savings("KB국민은행", "KB나만의 적금", 1_500_000L)),
                    List.of(
                            loan("KB국민은행", "KB 직장인든든 신용대출", 45_000_000L, 13_500_000L, "8.90"),
                            loan("한국장학재단", "취업 후 상환 학자금대출", 12_000_000L, 2_400_000L, "4.00")),
                    noAccounts(),
                    fetchedAt);
        }
    },

    /** 한서연 · 자산가 3억 · 유일한 무부채 · 연금·IRP 보유. */
    RICH {
        @Override
        MyDataSnapshot build(LocalDateTime fetchedAt) {
            return new MyDataSnapshot(
                    List.of(
                            checking("KB국민은행", "KB국민ONE통장", 40_000_000L),
                            savings("KB국민은행", "KB Star 정기예금", 150_000_000L),
                            securities("KB증권", "채권형펀드", 60_000_000L),
                            securities("KB증권", "KB 연금저축펀드", 30_000_000L),
                            securities("KB증권", "KB 개인형IRP", 20_000_000L)),
                    List.of(),
                    accounts(
                            true, true, false,
                            30_000_000L, 20_000_000L,
                            6_000_000L, 3_000_000L, 0L),
                    fetchedAt);
        }
    },

    /** 오태윤 · 자산 250만 · 카드론 1,500만(17.9%) · 고금리 부채 보유 → 대출 추천이 막힌다. */
    HIGH_RATE_DEBT {
        @Override
        MyDataSnapshot build(LocalDateTime fetchedAt) {
            return new MyDataSnapshot(
                    List.of(
                            checking("카카오뱅크", "카카오뱅크 입출금통장", 800_000L),
                            savings("KB국민은행", "KB나만의 적금", 1_200_000L),
                            securities("KB증권", "해외주식 위탁계좌", 500_000L)),
                    List.of(
                            highRateLoan("KB국민카드", "KB국민카드 장기카드대출", 15_000_000L, 7_000_000L, "17.90")),
                    noAccounts(),
                    fetchedAt);
        }
    },

    /** 매핑에 없는 회원(직접 가입한 테스트 계정 등)이 받는 기본 시나리오. */
    DEFAULT {
        @Override
        MyDataSnapshot build(LocalDateTime fetchedAt) {
            return new MyDataSnapshot(
                    List.of(
                            checking("KB국민은행", "KB국민ONE통장", 5_000_000L),
                            savings("KB국민은행", "KB Star 정기예금", 25_000_000L),
                            securities("KB증권", "KB증권 종합위탁", 10_000_000L)),
                    List.of(loan("KB국민은행", "KB 직장인든든 신용대출", 10_000_000L, 3_000_000L, "5.50")),
                    noAccounts(),
                    fetchedAt);
        }
    };

    private static final String ELIGIBLE = "ELIGIBLE";

    abstract MyDataSnapshot build(LocalDateTime fetchedAt);

    private static MyDataAsset checking(String institution, String product, long balance) {
        return asset(institution, product, AssetCategory.BANK_CHECKING, balance);
    }

    private static MyDataAsset savings(String institution, String product, long balance) {
        return asset(institution, product, AssetCategory.BANK_SAVINGS, balance);
    }

    private static MyDataAsset securities(String institution, String product, long balance) {
        return asset(institution, product, AssetCategory.SECURITIES, balance);
    }

    private static MyDataAsset asset(
            String institution, String product, AssetCategory category, long balance) {
        return new MyDataAsset(institution, product, category, BigDecimal.valueOf(balance));
    }

    private static MyDataLoan loan(
            String institution, String product, long balance, long annualPayment, String rate) {
        return newLoan(institution, product, balance, annualPayment, rate, false);
    }

    private static MyDataLoan highRateLoan(
            String institution, String product, long balance, long annualPayment, String rate) {
        return newLoan(institution, product, balance, annualPayment, rate, true);
    }

    private static MyDataLoan newLoan(
            String institution,
            String product,
            long balance,
            long annualPayment,
            String rate,
            boolean highRateDebt) {
        return new MyDataLoan(
                institution,
                product,
                BigDecimal.valueOf(balance),
                BigDecimal.valueOf(annualPayment),
                new BigDecimal(rate),
                highRateDebt);
    }

    /** 연금저축·IRP·ISA 를 모두 보유하지 않은 회원. 절세 추천은 평가 대상(ELIGIBLE)으로 둔다. */
    private static MyDataPensionIsa noAccounts() {
        return accounts(false, false, false, 0L, 0L, 0L, 0L, 0L);
    }

    private static MyDataPensionIsa accounts(
            boolean hasPensionSaving,
            boolean hasIrp,
            boolean hasIsa,
            long pensionSavingBalance,
            long irpBalance,
            long pensionAnnualPayment,
            long irpAnnualPayment,
            long isaAnnualDeposit) {
        return new MyDataPensionIsa(
                hasPensionSaving,
                hasIrp,
                false,
                hasIsa,
                BigDecimal.valueOf(pensionSavingBalance),
                BigDecimal.valueOf(irpBalance),
                BigDecimal.valueOf(pensionAnnualPayment),
                BigDecimal.valueOf(irpAnnualPayment),
                BigDecimal.ZERO,
                BigDecimal.valueOf(isaAnnualDeposit),
                ELIGIBLE,
                ELIGIBLE);
    }
}
