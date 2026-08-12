package com.financematch.asset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.financematch.asset.domain.AssetCategory;
import com.financematch.asset.domain.MyDataAsset;
import com.financematch.asset.domain.MyDataLoan;
import com.financematch.asset.domain.MyDataSnapshot;
import com.financematch.auth.domain.Member;
import com.financematch.auth.mapper.MemberMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 데모 마이데이터가 <b>회원 번호가 아니라 이메일</b>로 결정되는지 확인한다.
 *
 * <p>기존 데모 커플(demo.a·demo.b)의 값이 {@code R__02_seed_demo.sql}의 {@code financial_summary}
 * 와 어긋나면 자산 화면의 총액과 항목별 합계가 달라지고, 자산 갱신 한 번으로 데모 궁합 점수의 전제가
 * 깨진다. 아래 두 테스트가 그 회귀를 막는다.
 */
@ExtendWith(MockitoExtension.class)
class SimpleMyDataProviderTest {

    @Mock
    private MemberMapper memberMapper;

    @Test
    void demoInviterMatchesSeededFinancialSummary() {
        MyDataSnapshot snapshot = fetchAs("demo.a@chaltteok.dev");

        assertEquals(new BigDecimal("90000000"), totalAsset(snapshot));
        assertEquals(
                new BigDecimal("4000000"),
                amountOf(snapshot, AssetCategory.BANK_CHECKING));
        assertEquals(new BigDecimal("20000000"), totalDebt(snapshot));
        assertEquals(new BigDecimal("6000000"), annualDebtPayment(snapshot));
        assertEquals(new BigDecimal("4.50"), snapshot.loans().get(0).interestRate());
        assertFalse(snapshot.loans().get(0).highRateDebt());

        assertTrue(snapshot.pensionIsa().hasPensionSaving());
        assertFalse(snapshot.pensionIsa().hasIrp());
        assertTrue(snapshot.pensionIsa().hasIsa());
        assertEquals(
                new BigDecimal("5000000"),
                snapshot.pensionIsa().pensionSavingBalance());
        assertEquals(
                new BigDecimal("6000000"),
                snapshot.pensionIsa().pensionAnnualPayment());
        assertEquals(
                new BigDecimal("10000000"),
                snapshot.pensionIsa().isaAnnualDeposit());
    }

    @Test
    void demoInviteeMatchesSeededFinancialSummary() {
        MyDataSnapshot snapshot = fetchAs("demo.b@chaltteok.dev");

        assertEquals(new BigDecimal("180000000"), totalAsset(snapshot));
        assertEquals(
                new BigDecimal("3000000"),
                amountOf(snapshot, AssetCategory.BANK_CHECKING));
        assertEquals(new BigDecimal("40000000"), totalDebt(snapshot));
        assertEquals(new BigDecimal("10000000"), annualDebtPayment(snapshot));
        assertEquals(new BigDecimal("5.20"), snapshot.loans().get(0).interestRate());

        assertTrue(snapshot.pensionIsa().hasPensionSaving());
        assertTrue(snapshot.pensionIsa().hasIrp());
        assertTrue(snapshot.pensionIsa().hasIsa());
        assertEquals(
                new BigDecimal("20000000"),
                snapshot.pensionIsa().pensionSavingBalance());
        assertEquals(new BigDecimal("15000000"), snapshot.pensionIsa().irpBalance());
        assertEquals(
                new BigDecimal("20000000"),
                snapshot.pensionIsa().isaAnnualDeposit());
    }

    @Test
    void givesEachDemoMemberItsOwnAssets() {
        assertEquals(
                new BigDecimal("120000000"),
                totalAsset(fetchAs("demo.saver@chaltteok.dev")));
        assertEquals(
                new BigDecimal("123000000"),
                totalAsset(fetchAs("demo.investor@chaltteok.dev")));
        assertEquals(
                new BigDecimal("3500000"),
                totalAsset(fetchAs("demo.newlywed@chaltteok.dev")));
        assertEquals(
                new BigDecimal("2400000"),
                totalAsset(fetchAs("demo.renter@chaltteok.dev")));
        assertEquals(
                new BigDecimal("300000000"),
                totalAsset(fetchAs("demo.rich@chaltteok.dev")));
        assertEquals(
                new BigDecimal("2500000"),
                totalAsset(fetchAs("demo.debt@chaltteok.dev")));
    }

    /** 자산은 사람에게 붙어 있으므로 회원 번호가 달라도 이메일이 같으면 같은 값이 나온다. */
    @Test
    void picksScenarioByEmailNotByMemberId() {
        Member third = memberWith("demo.saver@chaltteok.dev");
        Member ninetyNinth = memberWith("demo.saver@chaltteok.dev");
        when(memberMapper.findById(3L)).thenReturn(third);
        when(memberMapper.findById(99L)).thenReturn(ninetyNinth);

        SimpleMyDataProvider provider = new SimpleMyDataProvider(memberMapper);

        assertEquals(
                totalAsset(provider.fetch(3L)),
                totalAsset(provider.fetch(99L)));
    }

    @Test
    void marksOnlyHighRateDebtMemberAsHighRate() {
        assertTrue(
                fetchAs("demo.debt@chaltteok.dev").loans().get(0).highRateDebt());
        assertFalse(
                fetchAs("demo.newlywed@chaltteok.dev").loans().get(0).highRateDebt());
    }

    @Test
    void givesRenterTwoLoansAndRichNone() {
        assertEquals(2, fetchAs("demo.renter@chaltteok.dev").loans().size());
        assertTrue(fetchAs("demo.rich@chaltteok.dev").loans().isEmpty());
    }

    @Test
    void fallsBackToDefaultScenarioForUnknownEmail() {
        MyDataSnapshot snapshot = fetchAs("someone@example.com");

        assertEquals(new BigDecimal("40000000"), totalAsset(snapshot));
    }

    @Test
    void fallsBackToDefaultScenarioWhenMemberNotFound() {
        when(memberMapper.findById(404L)).thenReturn(null);

        MyDataSnapshot snapshot = new SimpleMyDataProvider(memberMapper).fetch(404L);

        assertEquals(new BigDecimal("40000000"), totalAsset(snapshot));
    }

    @Test
    void rejectsNullMemberId() {
        SimpleMyDataProvider provider = new SimpleMyDataProvider(memberMapper);

        assertThrows(IllegalArgumentException.class, () -> provider.fetch(null));
    }

    private MyDataSnapshot fetchAs(String email) {
        // mock 생성을 when(...) 인자 안에서 하면 스터빙이 중첩돼 UnfinishedStubbingException 이 난다.
        Member member = memberWith(email);
        when(memberMapper.findById(1L)).thenReturn(member);

        return new SimpleMyDataProvider(memberMapper).fetch(1L);
    }

    private Member memberWith(String email) {
        Member member = mock(Member.class);
        when(member.getEmail()).thenReturn(email);

        return member;
    }

    private BigDecimal totalAsset(MyDataSnapshot snapshot) {
        return snapshot.assets().stream()
                .map(MyDataAsset::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalDebt(MyDataSnapshot snapshot) {
        return snapshot.loans().stream()
                .map(MyDataLoan::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal annualDebtPayment(MyDataSnapshot snapshot) {
        return snapshot.loans().stream()
                .map(MyDataLoan::annualPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal amountOf(MyDataSnapshot snapshot, AssetCategory category) {
        return snapshot.assets().stream()
                .filter(asset -> asset.category() == category)
                .map(MyDataAsset::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
