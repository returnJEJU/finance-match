package com.financematch.product.mapper;

import com.financematch.product.domain.Product;
import io.lettuce.core.dynamic.annotation.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductMapper {
    List<Product> findAll();

    Product findById(@Param("id") Long id);

    List<Product> findByProductType(@Param("productType") String productType);
}
