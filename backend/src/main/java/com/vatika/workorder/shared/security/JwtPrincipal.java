package com.vatika.workorder.shared.security;

import com.vatika.workorder.identity.model.Role;

import java.util.UUID;

public record JwtPrincipal(UUID userId, String email, Role role, UUID clientId) {

    public boolean isClient() {
        return role.isClientRole();
    }
}