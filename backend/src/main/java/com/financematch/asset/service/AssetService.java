package com.financematch.asset.service;

import com.financematch.asset.domain.AssetCategory;
import com.financematch.asset.domain.FinancialSummary;
import com.financematch.asset.domain.MyDataAsset;
import com.financematch.asset.domain.MyDataLoan;
import com.financematch.asset.domain.MyDataPensionIsa;
import com.financematch.asset.domain.MyDataSnapshot;
import com.financematch.asset.domain.PensionIsaAccount;
import com.financematch.asset.dto.AssetAccountResponse;
import com.financematch.asset.dto.AssetLinkResponse;
import com.financematch.asset.dto.AssetSummaryResponse;
import com.financematch.asset.mapper.AssetLinkRow;
import com.financematch.asset.mapper.AssetMapper;
import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetService {

    private final AssetMapper assetMapper;
    private final MyDataProvider myDataProvider;

    @Transactional(readOnly = true)
    public AssetLinkResponse getAssets(Long memberId) {
        validateMemberId(memberId);

        AssetLinkRow linkedAsset = assetMapper.findLinkByMemberId(memberId);
        if (linkedAsset == null) {
            throw new ApiException(ErrorCode.ASSET_NOT_LINKED);
        }

        MyDataSnapshot snapshot = fetch(memberId);
        EnumMap<AssetCategory, BigDecimal> amountByCategory = amountByCategory(snapshot);
        MyDataPensionIsa source = snapshot.pensionIsa();

        return new AssetLinkResponse(
                linkedAsset.getFinancialAsset(),
                linkedAsset.getTotalDebt(),
                assetCount(snapshot),
                summaryList(amountByCategory),
                accountResponse(source),
                linkedAsset.getLinkedAt());
    }

    @Transactional
    public AssetLinkResponse link(Long memberId) {
        validateMemberId(memberId);

        if (assetMapper.existsByMemberId(memberId)) {
            throw new ApiException(ErrorCode.ASSET_ALREADY_LINKED);
        }

        MyDataSnapshot snapshot = fetch(memberId);
        AggregatedMyData aggregated = aggregate(memberId, snapshot);

        try {
            requireInsertedRow(
                    assetMapper.insertFinancialSummary(aggregated.financialSummary()),
                    "금융 요약");
            requireInsertedRow(
                    assetMapper.insertPensionIsaAccount(aggregated.pensionIsaAccount()),
                    "연금·ISA 요약");
        } catch (DuplicateKeyException exception) {
            throw new ApiException(ErrorCode.ASSET_ALREADY_LINKED);
        }

        return aggregated.response();
    }

    @Transactional
    public AssetLinkResponse refresh(Long memberId) {
        validateMemberId(memberId);

        MyDataSnapshot snapshot = fetch(memberId);
        AggregatedMyData aggregated = aggregate(memberId, snapshot);

        requireUpsertedRow(
                assetMapper.upsertFinancialSummary(aggregated.financialSummary()),
                "금융 요약");
        requireUpsertedRow(
                assetMapper.upsertPensionIsaAccount(aggregated.pensionIsaAccount()),
                "연금·ISA 요약");

        return aggregated.response();
    }

    private MyDataSnapshot fetch(Long memberId) {
        try {
            MyDataSnapshot snapshot = myDataProvider.fetch(memberId);
            if (snapshot == null) {
                throw new IllegalStateException("마이데이터 응답이 없습니다.");
            }
            return snapshot;
        } catch (RuntimeException exception) {
            log.warn("마이데이터 조회 실패: memberId={}", memberId, exception);
            throw new ApiException(ErrorCode.MYDATA_LINK_FAILED);
        }
    }

    private AggregatedMyData aggregate(Long memberId, MyDataSnapshot snapshot) {
        EnumMap<AssetCategory, BigDecimal> amountByCategory = amountByCategory(snapshot);

        BigDecimal totalAsset =
                amountByCategory.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebt =
                snapshot.loans().stream()
                        .map(MyDataLoan::balance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal annualDebtPayment =
                snapshot.loans().stream()
                        .map(MyDataLoan::annualPayment)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        FinancialSummary financialSummary =
                FinancialSummary.builder()
                        .memberId(memberId)
                        .financialAsset(totalAsset)
                        .totalDebt(totalDebt)
                        .availableBalance(amountByCategory.get(AssetCategory.BANK_CHECKING))
                        .annualDebtPayment(annualDebtPayment)
                        .averageInterestRate(weightedAverageInterestRate(snapshot.loans(), totalDebt))
                        .hasHighRateDebt(
                                snapshot.loans().stream().anyMatch(MyDataLoan::highRateDebt))
                        .linkedAt(snapshot.fetchedAt())
                        .build();

        MyDataPensionIsa source = snapshot.pensionIsa();
        PensionIsaAccount pensionIsaAccount =
                PensionIsaAccount.builder()
                        .memberId(memberId)
                        .hasPensionSaving(source.hasPensionSaving())
                        .hasIrp(source.hasIrp())
                        .hasDc(source.hasDc())
                        .hasIsa(source.hasIsa())
                        .pensionSavingBalance(source.pensionSavingBalance())
                        .irpBalance(source.irpBalance())
                        .pensionAnnualPayment(source.pensionAnnualPayment())
                        .irpAnnualPayment(source.irpAnnualPayment())
                        .dcAnnualPayment(source.dcAnnualPayment())
                        .isaAnnualDeposit(source.isaAnnualDeposit())
                        .taxEligibilityStatus(source.taxEligibilityStatus())
                        .isaEligibilityStatus(source.isaEligibilityStatus())
                        .linkedAt(snapshot.fetchedAt())
                        .build();

        AssetLinkResponse response =
                new AssetLinkResponse(
                        totalAsset,
                        totalDebt,
                        assetCount(snapshot),
                        summaryList(amountByCategory),
                        accountResponse(source),
                        snapshot.fetchedAt());

        return new AggregatedMyData(financialSummary, pensionIsaAccount, response);
    }

    private EnumMap<AssetCategory, BigDecimal> amountByCategory(MyDataSnapshot snapshot) {
        EnumMap<AssetCategory, BigDecimal> amountByCategory =
                new EnumMap<>(AssetCategory.class);
        for (AssetCategory category : AssetCategory.values()) {
            amountByCategory.put(category, BigDecimal.ZERO);
        }
        for (MyDataAsset asset : snapshot.assets()) {
            amountByCategory.merge(asset.category(), asset.balance(), BigDecimal::add);
        }

        return amountByCategory;
    }

    private BigDecimal weightedAverageInterestRate(
            List<MyDataLoan> loans,
            BigDecimal totalDebt) {
        if (totalDebt.signum() == 0) {
            return null;
        }

        BigDecimal weightedRateSum =
                loans.stream()
                        .map(loan -> loan.balance().multiply(loan.interestRate()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return weightedRateSum.divide(totalDebt, 2, RoundingMode.HALF_UP);
    }

    private AssetSummaryResponse summary(
            AssetCategory category,
            EnumMap<AssetCategory, BigDecimal> amountByCategory) {
        return new AssetSummaryResponse(category, amountByCategory.get(category));
    }

    private List<AssetSummaryResponse> summaryList(
            EnumMap<AssetCategory, BigDecimal> amountByCategory) {
        return List.of(
                summary(AssetCategory.BANK_CHECKING, amountByCategory),
                summary(AssetCategory.BANK_SAVINGS, amountByCategory),
                summary(AssetCategory.SECURITIES, amountByCategory),
                summary(AssetCategory.INSURANCE, amountByCategory));
    }

    private AssetAccountResponse accountResponse(MyDataPensionIsa source) {
        return new AssetAccountResponse(
                source.hasPensionSaving(),
                source.hasIrp(),
                source.hasIsa());
    }

    private int assetCount(MyDataSnapshot snapshot) {
        return snapshot.assets().size() + snapshot.loans().size();
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }
    }

    private void requireInsertedRow(int affectedRows, String target) {
        if (affectedRows != 1) {
            throw new IllegalStateException(target + " 저장에 실패했습니다.");
        }
    }

    private void requireUpsertedRow(int affectedRows, String target) {
        if (affectedRows < 0 || affectedRows > 2) {
            throw new IllegalStateException(target + " 갱신에 실패했습니다.");
        }
    }

    private record AggregatedMyData(
            FinancialSummary financialSummary,
            PensionIsaAccount pensionIsaAccount,
            AssetLinkResponse response) {
    }
}
