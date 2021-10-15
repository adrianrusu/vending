package com.adrianr.vending.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {

    SELLER("SELLER"),
    BUYER("BUYER");

    private final String name;

}
