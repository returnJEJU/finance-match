package com.financematch.product.domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ProductDomainTest {

    @Test
    void createsProductDomainObjects() {
        assertNotNull(new Product());
        assertNotNull(new Deposit());
        assertNotNull(new Loan());
        assertNotNull(new Investment());
        assertNotNull(new TaxSaving());
    }
}
