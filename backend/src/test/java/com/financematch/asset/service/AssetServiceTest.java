package com.financematch.asset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.asset.domain.FinancialSummary;
import com.financematch.asset.domain.PensionIsaAccount;
import com.financematch.asset.dto.AssetLinkResponse;
import com.financematch.asset.mapper.AssetMapper;
import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import java.math.BigDecimal;
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
        assetService = new AssetService(assetMapper, new SimpleMyDataProvider());
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
    void storesHousingLoanOnlyForEvenMemberScenario() {
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
    void refreshesBothSummariesWithSameMockSnapshot() {
        when(assetMapper.upsertFinancialSummary(any())).thenReturn(2);
        when(assetMapper.upsertPensionIsaAccount(any())).thenReturn(2);

        AssetLinkResponse response = assetService.refresh(7L);

        verify(assetMapper).upsertFinancialSummary(any());
        verify(assetMapper).upsertPensionIsaAccount(any());
        assertEquals(new BigDecimal("84200000"), response.totalAsset());
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
}
