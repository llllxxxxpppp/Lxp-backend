package com.lcs.lxp.subscription.application;

import com.lcs.lxp.subscription.application.dto.response.SubscriptionResponse;
import com.lcs.lxp.subscription.application.service.SubscriptionService;
import com.lcs.lxp.subscription.domain.event.PaymentRequestedEvent;
import com.lcs.lxp.subscription.domain.exception.SubscriptionException;
import com.lcs.lxp.subscription.domain.model.entity.Payment;
import com.lcs.lxp.subscription.domain.model.entity.Subscription;
import com.lcs.lxp.subscription.domain.model.vo.RequestType;
import com.lcs.lxp.subscription.domain.model.vo.ResponseResult;
import com.lcs.lxp.subscription.domain.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SUB-03: SubscriptionService의 이벤트 기반 결제 흐름 전환 검증.
 *
 * <p>SubscriptionService는 더 이상 PaymentAdapter를 직접 호출하지 않는다. 무료 구독권은
 * 즉시 결제가 성공 처리되어 활성화되고, 유료 구독권은 PaymentRequestedEvent를 발행한 뒤
 * (아직 비활성 상태로) 저장된다. 실제 결제 결과 반영은 PaymentAdapter의 이벤트 리스너
 * 책임이며(PaymentAdapterTest 참고), 이 테스트는 ApplicationEventPublisher를 Mock으로
 * 대체하므로 리스너는 호출되지 않는다. suspendSubscription/reissueExpiring은 SUB-06/
 * SUB-07에서 재작성될 예정이므로 이 테스트 범위에 포함하지 않는다.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final Long FREE_PRICE = 0L;
    private static final Long PAID_PRICE = 9_900L;
    private static final Long SUBSCRIPTION_ID = 1L;
    private static final Long PAYMENT_ID = 100L;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    // -------------------------------------------------------------------------
    // createSubscription
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("가격이 0이면 결제가 즉시 성공 처리되어 구독권이 즉시 활성화되고 이벤트는 발행되지 않는다")
    void givenZeroPrice_whenCreateSubscription_thenPaymentSucceedsImmediatelyAndSubscriptionActivatedWithoutEvent() {
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            if (ReflectionTestUtils.getField(sub, "id") == null) {
                setId(sub, SUBSCRIPTION_ID);
            }
            return sub;
        });

        SubscriptionResponse response = subscriptionService.createSubscription(MEMBER_ID, FREE_PRICE);

        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, atLeastOnce()).save(subscriptionCaptor.capture());
        Subscription saved = subscriptionCaptor.getValue();

        assertNotNull(saved.getActivatedAt());
        assertEquals(1, saved.getPayments().size());
        Payment payment = saved.getPayments().get(0);
        assertEquals(RequestType.PAYMENT, payment.getRequestType());
        assertEquals(ResponseResult.SUCCESS, payment.getResponseResult());
        assertNotNull(payment.getRequestedAt());
        assertNotNull(payment.getRespondedAt());
        assertEquals(SUBSCRIPTION_ID, response.subscriptionId());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("가격이 0보다 크면 결제 요청 이벤트가 발행되고, 이 시점에는 구독권이 아직 활성화되지 않은 채 저장된다")
    void givenPositivePrice_whenCreateSubscription_thenPaymentRequestedEventPublishedAndSubscriptionNotYetActivated() {
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            if (ReflectionTestUtils.getField(sub, "id") == null) {
                setId(sub, SUBSCRIPTION_ID);
            }
            for (Payment payment : sub.getPayments()) {
                if (ReflectionTestUtils.getField(payment, "id") == null) {
                    setId(payment, PAYMENT_ID);
                }
            }
            return sub;
        });

        SubscriptionResponse response = subscriptionService.createSubscription(MEMBER_ID, PAID_PRICE);

        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, atLeastOnce()).save(subscriptionCaptor.capture());
        Subscription saved = subscriptionCaptor.getValue();

        assertNull(saved.getActivatedAt());
        assertEquals(1, saved.getPayments().size());
        Payment payment = saved.getPayments().get(0);
        assertEquals(RequestType.PAYMENT, payment.getRequestType());
        assertEquals(ResponseResult.NOT_REQUESTED, payment.getResponseResult());

        ArgumentCaptor<PaymentRequestedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PaymentRequestedEvent event = eventCaptor.getValue();
        assertEquals(SUBSCRIPTION_ID, event.getSubscriptionId());
        assertEquals(PAYMENT_ID, event.getPaymentId());
        assertEquals(SUBSCRIPTION_ID, response.subscriptionId());
    }

    // -------------------------------------------------------------------------
    // cancelSubscription
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("본인 소유 구독권을 취소하면 구독권이 취소 상태가 된다")
    void givenOwnSubscription_whenCancelSubscription_thenSubscriptionIsCancelled() {
        Subscription subscription = Subscription.create(MEMBER_ID, PAID_PRICE);
        subscription.activate();
        setId(subscription, SUBSCRIPTION_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        subscriptionService.cancelSubscription(MEMBER_ID, SUBSCRIPTION_ID);

        assertNotNull(subscription.getCancelledAt());
        verify(subscriptionRepository).save(subscription);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("본인 소유가 아닌 구독권을 취소하려 하면 예외가 발생하고 구독권은 취소되지 않는다")
    void givenOtherMembersSubscription_whenCancelSubscription_thenThrowsExceptionAndNotCancelled() {
        Subscription subscription = Subscription.create(MEMBER_ID, PAID_PRICE);
        subscription.activate();
        setId(subscription, SUBSCRIPTION_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        assertThrows(SubscriptionException.class,
                () -> subscriptionService.cancelSubscription(OTHER_MEMBER_ID, SUBSCRIPTION_ID));

        assertNull(subscription.getCancelledAt());
        verify(subscriptionRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getSubscriptionInfo
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("존재하는 구독권을 조회하면 매핑된 응답을 반환한다")
    void givenExistingSubscription_whenGetSubscriptionInfo_thenReturnsMappedResponse() {
        Subscription subscription = Subscription.create(MEMBER_ID, PAID_PRICE);
        subscription.activate();
        setId(subscription, SUBSCRIPTION_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.getSubscriptionInfo(SUBSCRIPTION_ID);

        assertEquals(SUBSCRIPTION_ID, response.subscriptionId());
        assertEquals(MEMBER_ID, response.memberId());
    }

    @Test
    @DisplayName("존재하지 않는 구독권을 조회하면 예외가 발생한다")
    void givenNonExistentSubscription_whenGetSubscriptionInfo_thenThrowsException() {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        assertThrows(SubscriptionException.class,
                () -> subscriptionService.getSubscriptionInfo(SUBSCRIPTION_ID));
    }
}
