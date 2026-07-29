package com.financematch.product.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class Product {
    private Long id;
    private String productType;
    private String productName;
    private String description;
    private String url;
    private String companyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
