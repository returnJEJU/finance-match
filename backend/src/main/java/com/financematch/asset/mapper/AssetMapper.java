package com.financematch.asset.mapper;

import com.financematch.asset.domain.FinancialSummary;
import com.financematch.asset.domain.PensionIsaAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AssetMapper {

    boolean existsByMemberId(Long memberId);

    int insertFinancialSummary(FinancialSummary financialSummary);

    int insertPensionIsaAccount(PensionIsaAccount pensionIsaAccount);

    int upsertFinancialSummary(FinancialSummary financialSummary);

    int upsertPensionIsaAccount(PensionIsaAccount pensionIsaAccount);
}
