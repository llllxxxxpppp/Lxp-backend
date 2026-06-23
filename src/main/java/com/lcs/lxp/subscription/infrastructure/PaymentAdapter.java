package com.lcs.lxp.subscription.infrastructure;

import com.lcs.lxp.subscription.domain.model.entity.Payment;
import com.lcs.lxp.subscription.domain.model.vo.PaymentFailureResponse;
import com.lcs.lxp.subscription.domain.model.vo.PaymentSuccessResponse;
import com.lcs.lxp.subscription.domain.model.vo.RefundFailureResponse;
import com.lcs.lxp.subscription.domain.model.vo.RefundInfo;
import com.lcs.lxp.subscription.domain.model.vo.RefundSuccessResponse;
import com.lcs.lxp.subscription.domain.repository.PaymentRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class PaymentAdapter {

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;

    public PaymentAdapter(PaymentGateway paymentGateway, PaymentRepository paymentRepository) {
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
    }

    public PaymentResult requestPayment(Payment payment) {
        payment.requestPayment();
        paymentRepository.save(payment);

        boolean success = paymentGateway.pay(payment.getIdempotencyKey(), payment.getAmount());
        if (success) {
            PaymentSuccessResponse response = new PaymentSuccessResponse(payment.getId(), OffsetDateTime.now());
            return PaymentResult.success(response);
        }
        PaymentFailureResponse response = new PaymentFailureResponse(payment.getId(), "결제 거부됨", OffsetDateTime.now());
        return PaymentResult.failure(response);
    }

    public void requestRefund(Payment payment) {
        RefundInfo refundInfo = new RefundInfo(payment.getId(), payment.getAmount());
        payment.requestRefund(refundInfo);
        paymentRepository.save(payment);

        boolean success = paymentGateway.refund(payment.getIdempotencyKey());
        if (success) {
            RefundSuccessResponse response = new RefundSuccessResponse(payment.getId(), OffsetDateTime.now());
            payment.handleRefundSuccess(response);
        } else {
            RefundFailureResponse response = new RefundFailureResponse(payment.getId(), "환불 거부됨", OffsetDateTime.now());
            payment.handleRefundFailure(response);
        }
        paymentRepository.save(payment);
    }
}
