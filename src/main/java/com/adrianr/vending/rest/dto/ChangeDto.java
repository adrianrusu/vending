package com.adrianr.vending.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ChangeDto {

    @Builder.Default
    private Integer newDeposit = 0;

    private Map<Integer, Integer> change;

}
