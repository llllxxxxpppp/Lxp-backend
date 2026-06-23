package com.lcs.lxp.subscription.presentation;

import com.lcs.lxp.subscription.domain.exception.SubscriptionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SubscriptionExceptionHandler {

    @ExceptionHandler(SubscriptionException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionException(SubscriptionException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
}
