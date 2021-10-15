package com.adrianr.vending.rest.dto;

import com.adrianr.vending.domain.User;
import com.adrianr.vending.domain.UserRole;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class CreateUserDto {

    @NotNull
    private String username;
    @NotNull
    private String password;
    @NotNull
    private UserRole role;

    public User toUser() {
        return User.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();
    }

}
