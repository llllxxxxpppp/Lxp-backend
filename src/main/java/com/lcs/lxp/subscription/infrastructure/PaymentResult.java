package com.lcs.lxp.subscription.infrastructure;

import com.lcs.lxp.subscription.domain.model.vo.PaymentFailureResponse;
import com.lcs.lxp.subscription.domain.model.vo.PaymentSuccessResponse;

public record PaymentResult(boolean success, PaymentSuccessResponse successResponse, PaymentFailureResponse failureResponse) {

    public static PaymentResult success(PaymentSuccessResponse response) {
        return new PaymentResult(true, response, null);
    }

    public static PaymentResult failure(PaymentFailureResponse response) {
        return new PaymentResult(false, null, response);
    }
}
