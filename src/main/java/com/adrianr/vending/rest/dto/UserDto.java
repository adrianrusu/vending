package com.adrianr.vending.rest.dto;

import com.adrianr.vending.domain.User;
import com.adrianr.vending.domain.UserRole;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserDto {

    private Integer id;
    private String username;
    private UserRole role;
    private BigDecimal deposit;

    public static UserDto fromUser(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .deposit(user.getDeposit())
                .build();
    }

}
