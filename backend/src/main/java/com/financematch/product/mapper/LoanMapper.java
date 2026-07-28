package com.financematch.product.mapper;

import com.financematch.product.dto.LoanProduct;
import com.financematch.product.type.LoanPurpose;
import com.financematch.product.type.LoanTargetGroup;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface LoanMapper {

    List<LoanProduct> findAll();

    LoanProduct findByProductId(
            @Param("productId") Long productId);

    List<LoanProduct> findByPurpose(
            @Param("loanPurpose") LoanPurpose loanPurpose);

    List<LoanProduct> findByTargetGroup(
            @Param("targetGroup") LoanTargetGroup targetGroup);
}