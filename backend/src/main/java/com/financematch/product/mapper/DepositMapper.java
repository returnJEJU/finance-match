package com.financematch.product.mapper;

import com.financematch.product.domain.DepositRate;
import com.financematch.product.dto.DepositProduct;
import com.financematch.product.type.DepositType;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DepositMapper {

    List<DepositProduct> findAll();

    List<DepositProduct> findByType(
            @Param("type") DepositType type);

    DepositProduct findByProductId(
            @Param("productId") Long productId);

    DepositRate findApplicableRate(
            @Param("productId") Long productId,
            @Param("term") Integer term);
}