package com.lcs.lxp.member.exception;

import com.lcs.lxp.common.exception.DomainException;

public class MemberException extends DomainException {

    private static final long serialVersionUID = 1L;

    public MemberException(String message) {
        super(message);
    }
}
