package com.lcs.lxp.course.exception;

import com.lcs.lxp.common.exception.DomainException;

public class CourseException extends DomainException {

    private static final long serialVersionUID = 1L;

    public CourseException(String message) {
        super(message);
    }
}
