package com.lcs.lxp.subscription.domain.exception;

import com.lcs.lxp.common.exception.DomainException;

public class SubscriptionException extends DomainException {

    private static final long serialVersionUID = 1L;

    public SubscriptionException(String message) {
        super(message);
    }
}
