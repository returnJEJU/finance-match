package com.financematch.product.mapper;

import com.financematch.product.dto.InvestmentProduct;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface InvestmentMapper {

    List<InvestmentProduct> findAll();

    InvestmentProduct findByProductId(
            @Param("productId") Long productId);

    List<InvestmentProduct> findByRiskLevel(
            @Param("riskLevel") Integer riskLevel);

    List<InvestmentProduct> findTdf();
}