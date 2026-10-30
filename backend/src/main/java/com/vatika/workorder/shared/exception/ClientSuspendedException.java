package com.vatika.workorder.shared.exception;

public class ClientSuspendedException extends RuntimeException {
    public ClientSuspendedException() {
        super("Account suspended. Contact support.");
    }
}