package com.halcyon.uniplace.service;

import java.util.List;

public class NotEligibleException extends BusinessRuleException {

    private final List<String> reasons;

    public NotEligibleException(List<String> reasons) {
        super("You are not eligible for this posting");
        this.reasons = List.copyOf(reasons);
    }

    public List<String> getReasons() {
        return reasons;
    }
}
