package com.lcs.lxp.security.exception;

import java.io.Serial;

public class SuspendedInstructorException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SuspendedInstructorException(String message) {
        super(message);
    }
}
