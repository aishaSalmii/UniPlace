package com.halcyon.uniplace.service;

/** A request that is well formed but not allowed by the business rules. The message is shown to the user. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
