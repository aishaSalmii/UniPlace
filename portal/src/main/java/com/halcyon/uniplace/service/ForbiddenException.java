package com.halcyon.uniplace.service;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException() {
        super("Access denied");
    }
}
