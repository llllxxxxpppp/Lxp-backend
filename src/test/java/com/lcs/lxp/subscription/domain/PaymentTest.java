package com.lcs.lxp.subscription.domain;

import com.lcs.lxp.subscription.domain.exception.SubscriptionException;
import com.lcs.lxp.subscription.domain.model.entity.Payment;
import com.lcs.lxp.subscription.domain.model.vo.PaymentFailureResponse;
import com.lcs.lxp.subscription.domain.model.vo.PaymentId;
import com.lcs.lxp.subscription.domain.model.vo.PaymentInfo;
import com.lcs.lxp.subscription.domain.model.vo.PaymentStatus;
import com.lcs.lxp.subscription.domain.model.vo.PaymentSuccessResponse;
import com.lcs.lxp.subscription.domain.model.vo.RefundFailureResponse;
import com.lcs.lxp.subscription.domain.model.vo.RefundInfo;
import com.lcs.lxp.subscription.domain.model.vo.RefundSuccessResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentTest {

    private Payment payment;
    private PaymentId paymentId;

    @BeforeEach
    void setUp() {
        payment = Payment.create(1L, new PaymentInfo("idempotency-key", 9900));
        ReflectionTestUtils.setField(payment, "id", 1L);
        paymentId = payment.getId();
    }

    @Test
    @DisplayName("멱등키가 비어있으면 PaymentInfo 생성 시 예외가 발생한다")
    void givenBlankIdempotencyKey_whenCreatePaymentInfo_thenThrowsException() {
        assertThrows(SubscriptionException.class, () -> new PaymentInfo("", 9900));
    }

    @Test
    @DisplayName("멱등키가 null이면 PaymentInfo 생성 시 예외가 발생한다")
    void givenNullIdempotencyKey_whenCreatePaymentInfo_thenThrowsException() {
        assertThrows(SubscriptionException.class, () -> new PaymentInfo(null, 9900));
    }

    @Test
    @DisplayName("금액이 음수이면 PaymentInfo 생성 시 예외가 발생한다")
    void givenNegativeAmount_whenCreatePaymentInfo_thenThrowsException() {
        assertThrows(SubscriptionException.class, () -> new PaymentInfo("key", -1));
    }

    @Test
    @DisplayName("결제가 생성되면 초기 상태는 PAYMENT_NOT_REQUESTED이다")
    void givenValidPaymentInfo_whenCreatePayment_thenStatusIsPaymentNotRequested() {
        Payment newPayment = Payment.create(1L, new PaymentInfo("key", 9900));

        assertEquals(PaymentStatus.PAYMENT_NOT_REQUESTED, newPayment.getStatus());
        assertNotNull(newPayment.getCreatedAt());
    }

    @Test
    @DisplayName("금액이 0이면 isFree가 true를 반환한다")
    void givenZeroAmount_whenCreatePayment_thenIsFreeIsTrue() {
        Payment freePayment = Payment.create(1L, new PaymentInfo("key", 0));

        assertTrue(freePayment.isFree());
    }

    @Test
    @DisplayName("금액이 0보다 크면 isFree가 false를 반환한다")
    void givenPositiveAmount_whenCreatePayment_thenIsFreeIsFalse() {
        assertFalse(payment.isFree());
    }

    @Test
    @DisplayName("결제 요청 시 상태가 PAYMENT_REQUESTED로 변경된다")
    void givenCreatedPayment_whenRequestPayment_thenStatusIsPaymentRequested() {
        payment.requestPayment();

        assertEquals(PaymentStatus.PAYMENT_REQUESTED, payment.getStatus());
        assertNotNull(payment.getUpdatedAt());
    }

    @Test
    @DisplayName("결제 성공 처리 시 상태가 PAYMENT_SUCCESS로 변경된다")
    void givenRequestedPayment_whenHandleSuccess_thenStatusIsPaymentSuccess() {
        payment.requestPayment();
        PaymentSuccessResponse response = new PaymentSuccessResponse(paymentId, OffsetDateTime.now());

        payment.handleSuccess(response);

        assertEquals(PaymentStatus.PAYMENT_SUCCESS, payment.getStatus());
    }

    @Test
    @DisplayName("결제 실패 처리 시 상태가 PAYMENT_FAILED로 변경된다")
    void givenRequestedPayment_whenHandleFailure_thenStatusIsPaymentFailed() {
        payment.requestPayment();
        PaymentFailureResponse response = new PaymentFailureResponse(paymentId, "잔액 부족", OffsetDateTime.now());

        payment.handleFailure(response);

        assertEquals(PaymentStatus.PAYMENT_FAILED, payment.getStatus());
    }

    @Test
    @DisplayName("이미 성공 처리된 결제에 재차 성공 처리하면 예외가 발생한다")
    void givenSuccessPayment_whenHandleSuccessAgain_thenThrowsException() {
        payment.requestPayment();
        PaymentSuccessResponse response = new PaymentSuccessResponse(paymentId, OffsetDateTime.now());
        payment.handleSuccess(response);

        assertThrows(SubscriptionException.class, () -> payment.handleSuccess(response));
    }

    @Test
    @DisplayName("이미 성공 처리된 결제에 실패 처리하면 예외가 발생한다")
    void givenSuccessPayment_whenHandleFailureAgain_thenThrowsException() {
        payment.requestPayment();
        payment.handleSuccess(new PaymentSuccessResponse(paymentId, OffsetDateTime.now()));
        PaymentFailureResponse failure = new PaymentFailureResponse(paymentId, "오류", OffsetDateTime.now());

        assertThrows(SubscriptionException.class, () -> payment.handleFailure(failure));
    }

    @Test
    @DisplayName("결제 성공 상태에서 환불 요청 시 상태가 REFUND_REQUESTED로 변경된다")
    void givenSuccessPayment_whenRequestRefund_thenStatusIsRefundRequested() {
        payment.requestPayment();
        payment.handleSuccess(new PaymentSuccessResponse(paymentId, OffsetDateTime.now()));
        RefundInfo refundInfo = new RefundInfo(paymentId, 9900);

        payment.requestRefund(refundInfo);

        assertEquals(PaymentStatus.REFUND_REQUESTED, payment.getStatus());
    }

    @Test
    @DisplayName("결제 성공 상태가 아닐 때 환불 요청하면 예외가 발생한다")
    void givenNonSuccessPayment_whenRequestRefund_thenThrowsException() {
        payment.requestPayment();
        RefundInfo refundInfo = new RefundInfo(paymentId, 9900);

        assertThrows(SubscriptionException.class, () -> payment.requestRefund(refundInfo));
    }

    @Test
    @DisplayName("환불 요청 후 환불 성공 처리 시 상태가 REFUND_SUCCESS로 변경된다")
    void givenRefundRequestedPayment_whenHandleRefundSuccess_thenStatusIsRefundSuccess() {
        payment.requestPayment();
        payment.handleSuccess(new PaymentSuccessResponse(paymentId, OffsetDateTime.now()));
        payment.requestRefund(new RefundInfo(paymentId, 9900));
        RefundSuccessResponse response = new RefundSuccessResponse(paymentId, OffsetDateTime.now());

        payment.handleRefundSuccess(response);

        assertEquals(PaymentStatus.REFUND_SUCCESS, payment.getStatus());
    }

    @Test
    @DisplayName("환불 요청 후 환불 실패 처리 시 상태가 REFUND_FAILED로 변경된다")
    void givenRefundRequestedPayment_whenHandleRefundFailure_thenStatusIsRefundFailed() {
        payment.requestPayment();
        payment.handleSuccess(new PaymentSuccessResponse(paymentId, OffsetDateTime.now()));
        payment.requestRefund(new RefundInfo(paymentId, 9900));
        RefundFailureResponse response = new RefundFailureResponse(paymentId, "환불 불가", OffsetDateTime.now());

        payment.handleRefundFailure(response);

        assertEquals(PaymentStatus.REFUND_FAILED, payment.getStatus());
    }

    @Test
    @DisplayName("이미 환불 완료된 결제에 재차 환불 요청하면 예외가 발생한다")
    void givenRefundedPayment_whenRequestRefundAgain_thenThrowsException() {
        payment.requestPayment();
        payment.handleSuccess(new PaymentSuccessResponse(paymentId, OffsetDateTime.now()));
        payment.requestRefund(new RefundInfo(paymentId, 9900));
        payment.handleRefundSuccess(new RefundSuccessResponse(paymentId, OffsetDateTime.now()));
        RefundInfo refundInfo = new RefundInfo(paymentId, 9900);

        assertThrows(SubscriptionException.class, () -> payment.requestRefund(refundInfo));
    }
}
