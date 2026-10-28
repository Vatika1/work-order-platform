package com.vatika.workorder.shared.tenancy;

import com.vatika.workorder.shared.exception.TenantContextMissingException;

import java.util.Optional;
import java.util.UUID;

//similar to SecurityContext to store current tenant/clientId per request. Tied to a single request thread. Stricter tenant isolation
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID clientId) {
        CURRENT.set(clientId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UUID require() {
        UUID id = CURRENT.get();
        if (id == null) {
            throw new TenantContextMissingException();
        }
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}