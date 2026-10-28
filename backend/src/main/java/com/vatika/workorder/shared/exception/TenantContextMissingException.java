package com.vatika.workorder.shared.exception;

public class TenantContextMissingException extends RuntimeException {
    public TenantContextMissingException() {
        super("This operation requires a client account");
    }
}