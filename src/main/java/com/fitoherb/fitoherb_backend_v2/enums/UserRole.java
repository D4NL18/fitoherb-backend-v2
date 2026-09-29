package com.fitoherb.fitoherb_backend_v2.enums;

public enum UserRole {

    ADMIN("admin"),

    SELLER("seller"),

    USER("user");

    private final String role;

    UserRole(String role)  {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

}
