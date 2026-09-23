package com.vatika.workorder.identity.model;

public enum Role {
    CLIENT_USER,
    CLIENT_ADMIN,
    STAFF,
    REVIEWER,
    PLATFORM_ADMIN;

    public boolean isClientRole() {
        return this == CLIENT_USER || this == CLIENT_ADMIN;
    }
}