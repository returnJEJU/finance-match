package com.financematch.product.mapper;

import com.financematch.config.RootConfig;
import com.financematch.product.dto.DepositProduct;
import com.financematch.product.type.DepositType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@WebAppConfiguration
class DepositMapperTest {

    @Autowired
    private DepositMapper depositMapper;

    @Test
    void 예금상품을_타입으로_조회한다() {

        List<DepositProduct> products =
                depositMapper.findByType(DepositType.DEPOSIT);

        assertNotNull(products);

        System.out.println("조회된 예금 상품 수 = " + products.size());

        products.forEach(System.out::println);
    }

    @Test
    void 적금상품을_타입으로_조회한다() {

        // when
        List<DepositProduct> products =
                depositMapper.findByType(DepositType.SAVINGS);

        // then
        assertNotNull(products);

        System.out.println("조회된 적금 상품 수 = " + products.size());

        products.forEach(System.out::println);
    }
}