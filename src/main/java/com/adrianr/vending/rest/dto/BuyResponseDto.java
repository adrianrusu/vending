package com.adrianr.vending.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BuyResponseDto {

    private BigDecimal totalPrice;
    private String productName;
    private Integer amount;
    private Integer change;

}
