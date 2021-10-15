package com.adrianr.vending.rest.dto;

import com.adrianr.vending.domain.Product;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ProductDto {

    @NotNull
    private String productName;
    @NotNull
    @Min(1)
    private BigDecimal cost;
    @Min(1)
    @NotNull
    private Integer amountAvailable;

    public Product toProduct() {
        return Product.builder()
                .productName(productName)
                .cost(cost)
                .amountAvailable(amountAvailable)
                .build();
    }

    public Product toProduct(Integer id) {
        return Product.builder()
                .id(id)
                .productName(productName)
                .cost(cost)
                .amountAvailable(amountAvailable)
                .build();
    }

}
