package com.financematch.asset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.asset.domain.AssetCategory;
import com.financematch.asset.domain.FinancialSummary;
import com.financematch.asset.domain.MyDataAsset;
import com.financematch.asset.domain.MyDataLoan;
import com.financematch.asset.domain.MyDataPensionIsa;
import com.financematch.asset.domain.MyDataSnapshot;
import com.financematch.asset.domain.PensionIsaAccount;
import com.financematch.asset.dto.AssetLinkResponse;
import com.financematch.asset.mapper.AssetLinkRow;
import com.financematch.asset.mapper.AssetMapper;
import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetMapper assetMapper;

    private AssetService assetService;

    @BeforeEach
    void setUp() {
        assetService = new AssetService(assetMapper, new FixedMyDataProvider());
    }

    @Test
    void linksMockDataAndStoresCalculatedSummaries() {
        when(assetMapper.insertFinancialSummary(any())).thenReturn(1);
        when(assetMapper.insertPensionIsaAccount(any())).thenReturn(1);

        AssetLinkResponse response = assetService.link(7L);

        ArgumentCaptor<FinancialSummary> financialCaptor =
                ArgumentCaptor.forClass(FinancialSummary.class);
        ArgumentCaptor<PensionIsaAccount> pensionCaptor =
                ArgumentCaptor.forClass(PensionIsaAccount.class);
        verify(assetMapper).insertFinancialSummary(financialCaptor.capture());
        verify(assetMapper).insertPensionIsaAccount(pensionCaptor.capture());

        FinancialSummary financial = financialCaptor.getValue();
        assertEquals(7L, financial.getMemberId());
        assertEquals(new BigDecimal("84200000"), financial.getFinancialAsset());
        assertEquals(BigDecimal.ZERO, financial.getTotalDebt());
        assertEquals(new BigDecimal("12630000"), financial.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, financial.getAnnualDebtPayment());
        assertNull(financial.getAverageInterestRate());

        PensionIsaAccount pension = pensionCaptor.getValue();
        assertEquals(new BigDecimal("4800000"), pension.getPensionAnnualPayment());
        assertEquals(new BigDecimal("6000000"), pension.getIsaAnnualDeposit());

        assertEquals(new BigDecimal("84200000"), response.totalAsset());
        assertEquals(BigDecimal.ZERO, response.totalDebt());
        assertEquals(5, response.assetCount());
        assertEquals(4, response.summary().size());
    }

    @Test
    void rejectsAlreadyLinkedMemberBeforeFetchingOrSaving() {
        when(assetMapper.existsByMemberId(7L)).thenReturn(true);

        ApiException exception =
                assertThrows(ApiException.class, () -> assetService.link(7L));

        assertEquals(ErrorCode.ASSET_ALREADY_LINKED, exception.getErrorCode());
        verify(assetMapper, never()).insertFinancialSummary(any());
        verify(assetMapper, never()).insertPensionIsaAccount(any());
    }

    @Test
    void storesDebtSummaryWhenSnapshotHasLoan() {
        when(assetMapper.insertFinancialSummary(any())).thenReturn(1);
        when(assetMapper.insertPensionIsaAccount(any())).thenReturn(1);

        AssetLinkResponse response = assetService.link(8L);

        ArgumentCaptor<FinancialSummary> financialCaptor =
                ArgumentCaptor.forClass(FinancialSummary.class);
        verify(assetMapper).insertFinancialSummary(financialCaptor.capture());

        FinancialSummary financial = financialCaptor.getValue();
        assertEquals(8L, financial.getMemberId());
        assertEquals(new BigDecimal("63000000"), financial.getFinancialAsset());
        assertEquals(new BigDecimal("35000000"), financial.getTotalDebt());
        assertEquals(new BigDecimal("3600000"), financial.getAnnualDebtPayment());
        assertEquals(new BigDecimal("4.15"), financial.getAverageInterestRate());
        assertEquals(new BigDecimal("35000000"), response.totalDebt());
    }

    @Test
    void refreshesBothSummariesWithSameSnapshot() {
        when(assetMapper.upsertFinancialSummary(any())).thenReturn(2);
        when(assetMapper.upsertPensionIsaAccount(any())).thenReturn(2);

        AssetLinkResponse response = assetService.refresh(7L);

        verify(assetMapper).upsertFinancialSummary(any());
        verify(assetMapper).upsertPensionIsaAccount(any());
        assertEquals(new BigDecimal("84200000"), response.totalAsset());
    }

    @Test
    void getsLinkedAssetsWithStoredTotalsAndLinkedAt() {
        LocalDateTime linkedAt = LocalDateTime.of(2026, 8, 5, 10, 30);
        AssetLinkRow stored =
                new AssetLinkRow(
                        new BigDecimal("84000000"),
                        new BigDecimal("1000000"),
                        linkedAt);
        when(assetMapper.findLinkByMemberId(7L)).thenReturn(stored);

        AssetLinkResponse response = assetService.getAssets(7L);

        assertEquals(new BigDecimal("84000000"), response.totalAsset());
        assertEquals(new BigDecimal("1000000"), response.totalDebt());
        assertEquals(5, response.assetCount());
        assertEquals(4, response.summary().size());
        assertEquals(linkedAt, response.linkedAt());
    }

    @Test
    void rejectsAssetLookupWhenMemberHasNotLinkedAssets() {
        when(assetMapper.findLinkByMemberId(7L)).thenReturn(null);

        ApiException exception =
                assertThrows(ApiException.class, () -> assetService.getAssets(7L));

        assertEquals(ErrorCode.ASSET_NOT_LINKED, exception.getErrorCode());
    }

    @Test
    void convertsProviderFailureToMyDataLinkError() {
        MyDataProvider failingProvider =
                memberId -> {
                    throw new IllegalStateException("목 서버 연결 실패");
                };
        AssetService failingService = new AssetService(assetMapper, failingProvider);

        ApiException exception =
                assertThrows(ApiException.class, () -> failingService.link(7L));

        assertEquals(ErrorCode.MYDATA_LINK_FAILED, exception.getErrorCode());
        verify(assetMapper, never()).insertFinancialSummary(any());
        verify(assetMapper, never()).insertPensionIsaAccount(any());
    }

    /**
     * 고정 스냅샷을 돌려주는 테스트용 마이데이터.
     *
     * <p>이 테스트가 검증하는 것은 {@link AssetService}의 합산·저장 로직이지 데모 시나리오가 아니다.
     * {@code MyDataScenario}의 값이 바뀌어도 이 테스트는 영향을 받지 않아야 하므로 provider 를 직접
     * 붙이지 않는다. 7L 은 무부채, 8L 은 대출 보유 회원이다.
     */
    private static final class FixedMyDataProvider implements MyDataProvider {

        @Override
        public MyDataSnapshot fetch(Long memberId) {
            return memberId == 8L ? withLoan() : debtFree();
        }

        private MyDataSnapshot debtFree() {
            return new MyDataSnapshot(
                    List.of(
                            asset(AssetCategory.BANK_CHECKING, 12_630_000L),
                            asset(AssetCategory.BANK_SAVINGS, 50_520_000L),
                            asset(AssetCategory.SECURITIES, 9_050_000L),
                            asset(AssetCategory.SECURITIES, 6_000_000L),
                            asset(AssetCategory.SECURITIES, 6_000_000L)),
                    List.of(),
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
                            "ELIGIBLE"),
                    LocalDateTime.of(2026, 8, 5, 10, 30));
        }

        private MyDataSnapshot withLoan() {
            return new MyDataSnapshot(
                    List.of(
                            asset(AssetCategory.BANK_CHECKING, 8_400_000L),
                            asset(AssetCategory.BANK_SAVINGS, 30_600_000L),
                            asset(AssetCategory.SECURITIES, 20_000_000L),
                            asset(AssetCategory.SECURITIES, 4_000_000L)),
                    List.of(
                            new MyDataLoan(
                                    "KB국민은행",
                                    "KB 전세금안심대출",
                                    BigDecimal.valueOf(35_000_000L),
                                    BigDecimal.valueOf(3_600_000L),
                                    new BigDecimal("4.15"),
                                    false)),
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
                            "ELIGIBLE"),
                    LocalDateTime.of(2026, 8, 5, 10, 30));
        }

        private MyDataAsset asset(AssetCategory category, long balance) {
            return new MyDataAsset(
                    "KB국민은행", "테스트 계좌", category, BigDecimal.valueOf(balance));
        }
    }
}
