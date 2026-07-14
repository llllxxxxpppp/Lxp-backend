package com.lcs.lxp.subscription.infrastructure;

import com.lcs.lxp.common.event.BaseDomainEvent;
import com.lcs.lxp.subscription.domain.event.PaymentRequestedEvent;
import com.lcs.lxp.subscription.domain.event.RefundRequestedEvent;
import com.lcs.lxp.subscription.domain.model.entity.Payment;
import com.lcs.lxp.subscription.domain.model.entity.Subscription;
import com.lcs.lxp.subscription.domain.model.vo.RequestType;
import com.lcs.lxp.subscription.domain.model.vo.ResponseResult;
import com.lcs.lxp.subscription.domain.repository.PaymentRepository;
import com.lcs.lxp.subscription.domain.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SUB-03: PaymentAdapter의 이벤트 리스너 기반 결제/환불 처리 검증.
 *
 * <p>PaymentAdapter는 {@code @EventListener}로 PaymentRequestedEvent / RefundRequestedEvent를
 * 구독하여, 구독권/결제 요청을 조회한 뒤 PaymentGateway(stub)를 호출하고 그 결과를 같은
 * 메서드 안에서 동기적으로 Payment/Subscription에 반영(저장까지)한다. 이 테스트는 스프링
 * 컨텍스트 없이 리스너 메서드를 직접 호출하여(handlePaymentRequested/handleRefundRequested)
 * 그 동작을 검증한다. RefundRequestedEvent를 실제로 발행하는 프로덕션 호출부(예:
 * cancelSubscription)는 이번 작업 범위가 아니며, 여기서는 리스너 자체의 동작만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentAdapterTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long PAID_PRICE = 9_900L;
    private static final Long SUBSCRIPTION_ID = 1L;
    private static final Long PAYMENT_ID = 100L;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentAdapter paymentAdapter;

    private Subscription subscription;

    private void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    @BeforeEach
    void setUp() {
        subscription = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(subscription, SUBSCRIPTION_ID);
    }

    // -------------------------------------------------------------------------
    // PaymentRequestedEvent 리스너
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("결제 요청 이벤트를 받아 PG 결제가 성공하면 결제가 SUCCESS로 기록되고 구독권이 활성화된다")
    void givenPaymentRequestedEvent_whenGatewayPaySucceeds_thenPaymentIsSuccessAndSubscriptionActivated() {
        Payment payment = Payment.create(RequestType.PAYMENT);
        setId(payment, PAYMENT_ID);
        subscription.addPayment(payment);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(paymentGateway.pay(anyString(), anyInt())).thenReturn(true);

        paymentAdapter.handlePaymentRequested(new PaymentRequestedEvent(SUBSCRIPTION_ID, PAYMENT_ID));

        assertNotNull(payment.getRequestedAt());
        assertNotNull(payment.getRespondedAt());
        assertEquals(ResponseResult.SUCCESS, payment.getResponseResult());
        assertNotNull(subscription.getActivatedAt());
        verify(paymentRepository).save(payment);
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    @DisplayName("결제 요청 이벤트를 받아 PG 결제가 실패하면 결제가 FAILED로 기록되고 구독권은 활성화되지 않는다")
    void givenPaymentRequestedEvent_whenGatewayPayFails_thenPaymentIsFailedAndSubscriptionNotActivated() {
        Payment payment = Payment.create(RequestType.PAYMENT);
        setId(payment, PAYMENT_ID);
        subscription.addPayment(payment);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(paymentGateway.pay(anyString(), anyInt())).thenReturn(false);

        paymentAdapter.handlePaymentRequested(new PaymentRequestedEvent(SUBSCRIPTION_ID, PAYMENT_ID));

        assertNotNull(payment.getRequestedAt());
        assertNotNull(payment.getRespondedAt());
        assertEquals(ResponseResult.FAILED, payment.getResponseResult());
        assertNull(subscription.getActivatedAt());
        verify(paymentRepository).save(payment);
    }

    // -------------------------------------------------------------------------
    // RefundRequestedEvent 리스너
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("환불 요청 이벤트를 받아 PG 환불이 성공하면 결제가 SUCCESS로 기록되고 구독권이 정지된다")
    void givenRefundRequestedEvent_whenGatewayRefundSucceeds_thenPaymentIsSuccessAndSubscriptionSuspended() {
        subscription.activate();
        Payment payment = Payment.create(RequestType.REFUND);
        setId(payment, PAYMENT_ID);
        subscription.addPayment(payment);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(paymentGateway.refund(anyString())).thenReturn(true);

        paymentAdapter.handleRefundRequested(new RefundRequestedEvent(SUBSCRIPTION_ID, PAYMENT_ID));

        assertNotNull(payment.getRequestedAt());
        assertNotNull(payment.getRespondedAt());
        assertEquals(ResponseResult.SUCCESS, payment.getResponseResult());
        assertNotNull(subscription.getSuspendedAt());
        verify(paymentRepository).save(payment);
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    @DisplayName("환불 요청 이벤트를 받아 PG 환불이 실패하면 결제가 FAILED로 기록되고 구독권은 정지되지 않는다")
    void givenRefundRequestedEvent_whenGatewayRefundFails_thenPaymentIsFailedAndSubscriptionNotSuspended() {
        subscription.activate();
        Payment payment = Payment.create(RequestType.REFUND);
        setId(payment, PAYMENT_ID);
        subscription.addPayment(payment);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(paymentGateway.refund(anyString())).thenReturn(false);

        paymentAdapter.handleRefundRequested(new RefundRequestedEvent(SUBSCRIPTION_ID, PAYMENT_ID));

        assertNotNull(payment.getRequestedAt());
        assertNotNull(payment.getRespondedAt());
        assertEquals(ResponseResult.FAILED, payment.getResponseResult());
        assertNull(subscription.getSuspendedAt());
        verify(paymentRepository).save(payment);
    }

    // -------------------------------------------------------------------------
    // 이벤트 구조 (BaseDomainEvent 상속 / 구독권 id, 결제 id 노출)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PaymentRequestedEvent는 BaseDomainEvent를 상속하고 구독권 id와 결제 id를 노출한다")
    void givenSubscriptionIdAndPaymentId_whenCreatePaymentRequestedEvent_thenExposesIdsAndInheritsBaseDomainEvent() {
        PaymentRequestedEvent event = new PaymentRequestedEvent(SUBSCRIPTION_ID, PAYMENT_ID);

        assertTrue(event instanceof BaseDomainEvent);
        assertEquals(SUBSCRIPTION_ID, event.getSubscriptionId());
        assertEquals(PAYMENT_ID, event.getPaymentId());
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
    }

    @Test
    @DisplayName("RefundRequestedEvent는 BaseDomainEvent를 상속하고 구독권 id와 결제 id를 노출한다")
    void givenSubscriptionIdAndPaymentId_whenCreateRefundRequestedEvent_thenExposesIdsAndInheritsBaseDomainEvent() {
        RefundRequestedEvent event = new RefundRequestedEvent(SUBSCRIPTION_ID, PAYMENT_ID);

        assertTrue(event instanceof BaseDomainEvent);
        assertEquals(SUBSCRIPTION_ID, event.getSubscriptionId());
        assertEquals(PAYMENT_ID, event.getPaymentId());
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
    }
}
