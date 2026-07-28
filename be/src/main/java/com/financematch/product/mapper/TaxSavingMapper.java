package com.financematch.product.mapper;

import com.financematch.product.dto.TaxSavingProduct;
import com.financematch.product.type.IsaType;
import com.financematch.product.type.TaxAccountType;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TaxSavingMapper {

    List<TaxSavingProduct> findAll();

    List<TaxSavingProduct> findByAccountType(
            @Param("accountType") TaxAccountType accountType);

    List<TaxSavingProduct> findIsaByType(
            @Param("isaType") IsaType isaType);

    TaxSavingProduct findByProductId(
            @Param("productId") Long productId);
}