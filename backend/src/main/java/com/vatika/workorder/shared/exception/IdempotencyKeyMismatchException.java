package com.vatika.workorder.shared.exception;

public class IdempotencyKeyMismatchException extends RuntimeException{
    public IdempotencyKeyMismatchException(String message) {
        super(message);
    }
}
