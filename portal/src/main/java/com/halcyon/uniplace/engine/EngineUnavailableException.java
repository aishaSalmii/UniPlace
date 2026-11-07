package com.halcyon.uniplace.engine;

import com.halcyon.uniplace.service.BusinessRuleException;

public class EngineUnavailableException extends BusinessRuleException {

    public EngineUnavailableException() {
        super("The matching service is not available at the moment. Nothing was changed; please try again later.");
    }
}
