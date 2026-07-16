package com.lcs.lxp.subscription.domain;

import com.lcs.lxp.subscription.domain.model.entity.Payment;
import com.lcs.lxp.subscription.domain.model.entity.Subscription;
import com.lcs.lxp.subscription.domain.model.vo.RequestId;
import com.lcs.lxp.subscription.domain.model.vo.RequestType;
import com.lcs.lxp.subscription.domain.model.vo.ResponseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SUB-02: Payment 애그리거트(요청 리스트 구조) 재설계 검증.
 *
 * <p>Payment는 데이터만 보관하는 엔티티다. 멱등키(RequestId)와 요청 타입
 * (RequestType: PAYMENT/REFUND)을 불변 필드로, 요청 전송 일시/응답 수신 일시/
 * 응답 결과(ResponseResult)를 가변 필드로 갖는다. 상태 전이 검증(이미 처리된 요청인지,
 * 유효한 응답인지 등)과 같은 비즈니스 로직은 인프라스트럭처 레이어의 어댑터 책임이며
 * 이번 범위(SUB-02, 도메인 모델)에 포함하지 않는다.
 * Subscription은 결제/환불 요청 내역을 List&lt;Payment&gt; 하나로 통합 관리한다.
 * 금액(가격) 관련 로직은 이 테스트 범위가 아니다(SUB-01에서 Subscription.getPrice()로 관리).
 */
class PaymentTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long PAID_PRICE = 19_800L;

    private Subscription subscription;

    @BeforeEach
    void setUp() {
        subscription = Subscription.create(MEMBER_ID, PAID_PRICE);
    }

    @Test
    @DisplayName("요청 타입으로 결제를 생성하면 멱등키가 자동 생성되고 요청 타입이 저장된다")
    void givenPaymentRequestType_whenCreate_thenRequestIdIsGeneratedAndRequestTypeIsStored() {
        Payment payment = Payment.create(RequestType.PAYMENT);

        assertNotNull(payment.getRequestId());
        assertEquals(RequestType.PAYMENT, payment.getRequestType());
    }

    @Test
    @DisplayName("환불 타입으로 결제를 생성하면 요청 타입이 REFUND로 저장된다")
    void givenRefundRequestType_whenCreate_thenRequestTypeIsRefund() {
        Payment payment = Payment.create(RequestType.REFUND);

        assertEquals(RequestType.REFUND, payment.getRequestType());
    }

    @Test
    @DisplayName("서로 다른 두 결제를 생성하면 멱등키가 서로 다르게 자동 생성된다")
    void givenTwoPayments_whenCreate_thenRequestIdsAreDifferent() {
        Payment first = Payment.create(RequestType.PAYMENT);
        Payment second = Payment.create(RequestType.PAYMENT);

        assertNotEquals(first.getRequestId(), second.getRequestId());
    }

    @Test
    @DisplayName("요청 타입이 null이면 결제 생성 시 예외가 발생한다")
    void givenNullRequestType_whenCreate_thenThrowsException() {
        assertThrows(NullPointerException.class, () -> Payment.create(null));
    }

    @Test
    @DisplayName("결제를 생성하면 요청 전송 일시와 응답 수신 일시는 NULL이고 응답 결과는 NOT_REQUESTED이다")
    void givenNewPayment_whenCreate_thenMutableFieldsHaveDefaultValues() {
        Payment payment = Payment.create(RequestType.PAYMENT);

        assertNull(payment.getRequestedAt());
        assertNull(payment.getRespondedAt());
        assertEquals(ResponseResult.NOT_REQUESTED, payment.getResponseResult());
    }

    @Test
    @DisplayName("결제를 생성하면 생성 일시는 채워지고 수정 일시는 NULL이다")
    void givenNewPayment_whenCreate_thenCreatedAtIsSetAndUpdatedAtIsNull() {
        Payment payment = Payment.create(RequestType.PAYMENT);

        assertNotNull(payment.getCreatedAt());
        assertNull(payment.getUpdatedAt());
    }

    @Test
    @DisplayName("요청 전송을 기록하면 요청 전송 일시와 수정 일시가 채워진다")
    void givenCreatedPayment_whenMarkRequested_thenRequestedAtAndUpdatedAtAreSet() {
        Payment payment = Payment.create(RequestType.PAYMENT);

        payment.markRequested();

        assertNotNull(payment.getRequestedAt());
        assertNotNull(payment.getUpdatedAt());
    }

    @Test
    @DisplayName("응답을 성공으로 기록하면 응답 수신 일시가 채워지고 응답 결과는 SUCCESS이다")
    void givenPayment_whenMarkRespondedWithSuccess_thenRespondedAtIsSetAndResultIsSuccess() {
        Payment payment = Payment.create(RequestType.PAYMENT);
        payment.markRequested();

        payment.markResponded(ResponseResult.SUCCESS);

        assertNotNull(payment.getRespondedAt());
        assertEquals(ResponseResult.SUCCESS, payment.getResponseResult());
    }

    @Test
    @DisplayName("응답을 실패로 기록하면 응답 수신 일시가 채워지고 응답 결과는 FAILED이다")
    void givenPayment_whenMarkRespondedWithFailed_thenRespondedAtIsSetAndResultIsFailed() {
        Payment payment = Payment.create(RequestType.PAYMENT);
        payment.markRequested();

        payment.markResponded(ResponseResult.FAILED);

        assertNotNull(payment.getRespondedAt());
        assertEquals(ResponseResult.FAILED, payment.getResponseResult());
    }

    @Test
    @DisplayName("응답 결과로 null을 전달하면 예외가 발생한다")
    void givenNullResult_whenMarkResponded_thenThrowsException() {
        Payment payment = Payment.create(RequestType.PAYMENT);
        payment.markRequested();

        assertThrows(NullPointerException.class, () -> payment.markResponded(null));
    }

    @Test
    @DisplayName("요청 전송과 응답 기록을 하더라도 멱등키와 요청 타입은 변하지 않는다 (불변 필드)")
    void givenCreatedPayment_whenMarkRequestedAndResponded_thenRequestIdAndRequestTypeRemainUnchanged() {
        Payment payment = Payment.create(RequestType.PAYMENT);
        RequestId originalRequestId = payment.getRequestId();
        RequestType originalRequestType = payment.getRequestType();

        payment.markRequested();
        payment.markResponded(ResponseResult.SUCCESS);

        assertEquals(originalRequestId, payment.getRequestId());
        assertEquals(originalRequestType, payment.getRequestType());
    }

    @Test
    @DisplayName("신규 구독권은 결제/환불 요청 내역 리스트가 비어있다")
    void givenNewSubscription_whenCreate_thenPaymentsListIsEmpty() {
        assertTrue(subscription.getPayments().isEmpty());
    }

    @Test
    @DisplayName("구독권에 결제 요청을 추가하면 리스트에 포함된다")
    void givenSubscription_whenAddPayment_thenPaymentsListContainsIt() {
        Payment payment = Payment.create(RequestType.PAYMENT);

        subscription.addPayment(payment);

        assertEquals(1, subscription.getPayments().size());
        assertTrue(subscription.getPayments().contains(payment));
    }

    @Test
    @DisplayName("구독권에 결제 요청과 환불 요청을 함께 추가하면 하나의 리스트에 통합되어 관리된다")
    void givenSubscription_whenAddPaymentAndRefund_thenBothAreStoredInSingleList() {
        Payment paymentRequest = Payment.create(RequestType.PAYMENT);
        Payment refundRequest = Payment.create(RequestType.REFUND);

        subscription.addPayment(paymentRequest);
        subscription.addPayment(refundRequest);

        assertEquals(2, subscription.getPayments().size());
        assertTrue(subscription.getPayments().contains(paymentRequest));
        assertTrue(subscription.getPayments().contains(refundRequest));
    }

    @Test
    @DisplayName("구독권에 null을 결제 요청으로 추가하면 예외가 발생한다")
    void givenNullPayment_whenAddPayment_thenThrowsException() {
        assertThrows(NullPointerException.class, () -> subscription.addPayment(null));
    }

    @Test
    @DisplayName("재발급된 구독권은 원본의 결제/환불 요청 내역을 물려받지 않고 빈 리스트로 시작한다")
    void givenSubscriptionWithPayment_whenReissue_thenNewSubscriptionHasEmptyPaymentsList() {
        subscription.addPayment(Payment.create(RequestType.PAYMENT));

        Subscription reissued = subscription.reissue(PAID_PRICE);

        assertTrue(reissued.getPayments().isEmpty());
        assertEquals(1, subscription.getPayments().size());
    }
}
