package com.halcyon.uniplace.engine;

import com.halcyon.uniplace.service.BusinessRuleException;

/** The Engine answered, but refused the request as invalid (HTTP 400); the reason is shown to the user (USA-01). */
public class EngineRejectedException extends BusinessRuleException {

    private final String field;

    public EngineRejectedException(String field, String reason) {
        super("The matching service refused the request: " + field + " " + reason + ".");
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
