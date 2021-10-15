package com.adrianr.vending.rest.dto;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class UpdateUserDto {

    @NotNull
    private String username;
    private String password;

}
