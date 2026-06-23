package com.lcs.lxp.subscription.application;

import com.lcs.lxp.subscription.application.dto.response.SubscriptionResponse;
import com.lcs.lxp.subscription.application.service.SubscriptionService;
import com.lcs.lxp.subscription.domain.exception.SubscriptionException;
import com.lcs.lxp.subscription.domain.model.entity.Payment;
import com.lcs.lxp.subscription.domain.model.entity.Subscription;
import com.lcs.lxp.subscription.domain.model.vo.PaymentId;
import com.lcs.lxp.subscription.domain.model.vo.PaymentInfo;
import com.lcs.lxp.subscription.domain.model.vo.PaymentSuccessResponse;
import com.lcs.lxp.subscription.domain.model.vo.SubscriptionStatus;
import com.lcs.lxp.subscription.domain.repository.PaymentRepository;
import com.lcs.lxp.subscription.domain.repository.SubscriptionRepository;
import com.lcs.lxp.subscription.infrastructure.PaymentAdapter;
import com.lcs.lxp.subscription.infrastructure.PaymentResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAdapter paymentAdapter;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private void setId(Object entity, Long idValue) {
        ReflectionTestUtils.setField(entity, "id", idValue);
    }

    @Test
    @DisplayName("기존 구독 이력이 없으면 무료 구독권이 생성되고 즉시 활성화된다")
    void givenNoExistingSubscription_whenCreateSubscription_thenCreatesFreeSub() {
        when(subscriptionRepository.existsByMemberId(1L)).thenReturn(false);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            setId(inv.getArgument(0), 1L);
            return inv.getArgument(0);
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            setId(inv.getArgument(0), 1L);
            return inv.getArgument(0);
        });

        SubscriptionResponse response = subscriptionService.createSubscription(1L);

        assertEquals(SubscriptionStatus.ACTIVE.name(), response.status());
        verify(paymentAdapter, never()).requestPayment(any());
    }

    @Test
    @DisplayName("기존 구독 이력이 있으면 유료 구독권이 생성되고 결제 성공 시 활성화된다")
    void givenExistingSubscription_whenCreateSubscription_thenCreatesPaidAndActive() {
        when(subscriptionRepository.existsByMemberId(1L)).thenReturn(true);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            setId(inv.getArgument(0), 1L);
            return inv.getArgument(0);
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            setId(inv.getArgument(0), 1L);
            return inv.getArgument(0);
        });
        PaymentSuccessResponse successResponse = new PaymentSuccessResponse(new PaymentId(1L), OffsetDateTime.now());
        when(paymentAdapter.requestPayment(any())).thenReturn(PaymentResult.success(successResponse));

        SubscriptionResponse response = subscriptionService.createSubscription(1L);

        assertEquals(SubscriptionStatus.ACTIVE.name(), response.status());
        verify(paymentAdapter).requestPayment(any());
    }

    @Test
    @DisplayName("구독권을 조회하면 응답 DTO를 반환한다")
    void givenExistingSubscription_whenGetSubscriptionInfo_thenReturnsResponse() {
        Subscription subscription = Subscription.create(1L);
        subscription.activate();
        setId(subscription, 1L);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.getSubscriptionInfo(1L);

        assertEquals(1L, response.subscriptionId());
        assertEquals(SubscriptionStatus.ACTIVE.name(), response.status());
    }

    @Test
    @DisplayName("존재하지 않는 구독권 조회 시 예외가 발생한다")
    void givenNonExistentSubscription_whenGetSubscriptionInfo_thenThrowsException() {
        when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(SubscriptionException.class, () -> subscriptionService.getSubscriptionInfo(999L));
    }

    @Test
    @DisplayName("유료 활성 구독권이 환불 기간 내이면 취소 시 환불이 요청된다")
    void givenPaidActiveSubWithinRefundPeriod_whenCancel_thenRefundRequested() {
        Subscription subscription = Subscription.create(1L);
        subscription.activate();
        setId(subscription, 1L);
        Payment payment = Payment.create(1L, new PaymentInfo("key", 9900));
        setId(payment, 1L);
        payment.requestPayment();
        payment.handleSuccess(new PaymentSuccessResponse(new PaymentId(1L), OffsetDateTime.now()));

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(paymentRepository.findBySubscriptionId(1L)).thenReturn(Optional.of(payment));

        subscriptionService.cancelSubscription(1L, 1L);

        verify(paymentAdapter).requestRefund(payment);
        assertEquals(SubscriptionStatus.CANCELLED, subscription.getStatus());
    }

    @Test
    @DisplayName("환불 기간이 초과된 구독권 취소 시 환불이 요청되지 않는다")
    void givenActiveSubOutsideRefundPeriod_whenCancel_thenNoRefund() {
        Subscription subscription = Subscription.create(1L);
        subscription.activate();
        ReflectionTestUtils.setField(subscription, "activatedAt", OffsetDateTime.now().minusDays(15));
        setId(subscription, 1L);
        Payment payment = Payment.create(1L, new PaymentInfo("key", 9900));
        setId(payment, 1L);
        payment.requestPayment();
        payment.handleSuccess(new PaymentSuccessResponse(new PaymentId(1L), OffsetDateTime.now()));

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(paymentRepository.findBySubscriptionId(1L)).thenReturn(Optional.of(payment));

        subscriptionService.cancelSubscription(1L, 1L);

        verify(paymentAdapter, never()).requestRefund(any());
        assertEquals(SubscriptionStatus.CANCELLED, subscription.getStatus());
    }

    @Test
    @DisplayName("무료 구독권 취소 시 환불이 요청되지 않는다")
    void givenFreeActiveSubWithinPeriod_whenCancel_thenNoRefund() {
        Subscription subscription = Subscription.create(1L);
        subscription.activate();
        setId(subscription, 1L);
        Payment freePayment = Payment.create(1L, new PaymentInfo("key", 0));
        setId(freePayment, 1L);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(paymentRepository.findBySubscriptionId(1L)).thenReturn(Optional.of(freePayment));

        subscriptionService.cancelSubscription(1L, 1L);

        verify(paymentAdapter, never()).requestRefund(any());
        assertEquals(SubscriptionStatus.CANCELLED, subscription.getStatus());
    }

    @Test
    @DisplayName("타인의 구독권 취소 시 예외가 발생한다")
    void givenOtherMembersSubscription_whenCancel_thenThrowsException() {
        Subscription subscription = Subscription.create(1L);
        subscription.activate();
        setId(subscription, 1L);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        assertThrows(SubscriptionException.class, () -> subscriptionService.cancelSubscription(2L, 1L));
    }

    @Test
    @DisplayName("활성 구독권이 있는 회원을 정지하면 구독권이 정지된다")
    void givenActiveSubscription_whenSuspend_thenStatusIsSuspended() {
        Subscription subscription = Subscription.create(1L);
        subscription.activate();
        setId(subscription, 1L);
        when(subscriptionRepository.findByMemberIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));

        subscriptionService.suspendSubscription(1L);

        assertEquals(SubscriptionStatus.SUSPENDED, subscription.getStatus());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    @DisplayName("활성 구독권이 없는 회원 정지 시 아무 처리도 하지 않는다")
    void givenNoActiveSubscription_whenSuspend_thenDoesNothing() {
        when(subscriptionRepository.findByMemberIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        subscriptionService.suspendSubscription(1L);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("만료 임박 구독권 재발급 시 기존 구독권이 폐기되고 새 구독권이 활성화된다")
    void givenExpiringSubscription_whenReissue_thenOldDiscardedAndNewActive() {
        Subscription expiring = Subscription.create(1L);
        expiring.activate();
        setId(expiring, 1L);
        when(subscriptionRepository.findByStatusAndExpiresAtBetween(any(), any(), any()))
                .thenReturn(List.of(expiring));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription sub = inv.getArgument(0);
            if (ReflectionTestUtils.getField(sub, "id") == null) {
                setId(sub, 2L);
            }
            return sub;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            setId(inv.getArgument(0), 2L);
            return inv.getArgument(0);
        });
        PaymentSuccessResponse successResponse = new PaymentSuccessResponse(new PaymentId(2L), OffsetDateTime.now());
        when(paymentAdapter.requestPayment(any())).thenReturn(PaymentResult.success(successResponse));

        subscriptionService.reissueExpiring();

        assertEquals(SubscriptionStatus.DISCARDED, expiring.getStatus());
    }
}
