package com.vibeport.auth.enums;

import lombok.Getter;

@Getter
public enum Tokens {
    ACCESS("access"),
    REFRESH("refresh");

    private final String value;

    Tokens(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
