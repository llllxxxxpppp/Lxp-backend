package com.lcs.lxp.subscription.application;

import com.lcs.lxp.subscription.application.dto.response.SubscriptionResponse;
import com.lcs.lxp.subscription.application.service.SubscriptionService;
import com.lcs.lxp.subscription.domain.event.PaymentRequestedEvent;
import com.lcs.lxp.subscription.domain.event.RefundRequestedEvent;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private static final Long FREE_SIBLING_SUBSCRIPTION_ID = 2L;
    private static final Long OTHER_PAID_SUBSCRIPTION_ID = 3L;
    private static final Long FOURTH_SUBSCRIPTION_ID = 4L;
    private static final Long PAYMENT_ID = 100L;
    private static final Long REISSUE_PRICE = 19_800L;
    private static final Long ELIGIBLE_SUBSCRIPTION_A_ID = 10L;
    private static final Long ELIGIBLE_SUBSCRIPTION_B_ID = 12L;
    private static final Long INELIGIBLE_SUBSCRIPTION_ID = 11L;
    private static final long FIRST_REISSUED_SUBSCRIPTION_ID = 20L;
    private static final long FIRST_REISSUED_PAYMENT_ID = 200L;

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
    // cancelSubscription (SUB-04 재작업: 환불 정책 3조건 - 유료 && 유료 구독권 정확히
    // 1개 && 활성 상태(isValid()) && 14일 이내(isWithinRefundPeriod()) - 모두 충족해야만
    // 환불 요청(Payment(REFUND) 추가/저장 + RefundRequestedEvent 발행)이 이루어지며, 이 경우
    // subscription.cancel()은 호출되지 않는다. 하나라도 불만족하면 이벤트/환불 요청 없이
    // subscription.cancel()만 호출된다.)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유료/활성/14일 이내이면서 회원의 유료 구독권이 정확히 1개이면 환불이 요청되고 취소(cancel)는 호출되지 않는다")
    void givenSolePaidActiveSubscriptionWithinRefundPeriod_whenCancelSubscription_thenRefundRequestedAndCancelNotInvoked() {
        Subscription subscription = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(subscription, SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(subscription, "activatedAt", OffsetDateTime.now().minusDays(1));

        Subscription freeSibling = Subscription.create(MEMBER_ID, FREE_PRICE);
        setId(freeSibling, FREE_SIBLING_SUBSCRIPTION_ID);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        lenient().when(subscriptionRepository.findByMemberId(MEMBER_ID))
                .thenReturn(List.of(subscription, freeSibling));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            for (Payment payment : sub.getPayments()) {
                if (ReflectionTestUtils.getField(payment, "id") == null) {
                    setId(payment, PAYMENT_ID);
                }
            }
            return sub;
        });

        subscriptionService.cancelSubscription(MEMBER_ID, SUBSCRIPTION_ID);

        assertNull(subscription.getCancelledAt());
        assertEquals(1, subscription.getPayments().size());
        Payment refundPayment = subscription.getPayments().get(0);
        assertEquals(RequestType.REFUND, refundPayment.getRequestType());
        verify(subscriptionRepository, atLeastOnce()).save(subscription);

        ArgumentCaptor<RefundRequestedEvent> eventCaptor = ArgumentCaptor.forClass(RefundRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        RefundRequestedEvent event = eventCaptor.getValue();
        assertEquals(SUBSCRIPTION_ID, event.getSubscriptionId());
        assertEquals(PAYMENT_ID, event.getPaymentId());
    }

    @Test
    @DisplayName("무료 구독권을 취소하면 활성화 후 14일 이내이더라도 환불 없이 구독권 취소만 수행된다")
    void givenFreeSubscription_whenCancelSubscription_thenNoRefundAndSubscriptionIsCancelled() {
        Subscription subscription = Subscription.create(MEMBER_ID, FREE_PRICE);
        setId(subscription, SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(subscription, "activatedAt", OffsetDateTime.now().minusDays(1));

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        lenient().when(subscriptionRepository.findByMemberId(MEMBER_ID))
                .thenReturn(List.of(subscription));

        subscriptionService.cancelSubscription(MEMBER_ID, SUBSCRIPTION_ID);

        assertNotNull(subscription.getCancelledAt());
        assertEquals(0, subscription.getPayments().size());
        verify(subscriptionRepository).save(subscription);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("유료/활성/14일 이내이더라도 회원의 유료 구독권이 2개 이상이면 환불 없이 구독권 취소만 수행된다")
    void givenMultiplePaidSubscriptionsForMember_whenCancelSubscription_thenNoRefundAndSubscriptionIsCancelled() {
        Subscription subscription = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(subscription, SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(subscription, "activatedAt", OffsetDateTime.now().minusDays(1));

        Subscription otherPaid = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(otherPaid, OTHER_PAID_SUBSCRIPTION_ID);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.findByMemberId(MEMBER_ID))
                .thenReturn(List.of(subscription, otherPaid));

        subscriptionService.cancelSubscription(MEMBER_ID, SUBSCRIPTION_ID);

        assertNotNull(subscription.getCancelledAt());
        assertEquals(0, subscription.getPayments().size());
        verify(subscriptionRepository).save(subscription);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("유료/활성/유료 구독권 1개이더라도 활성화 후 14일이 지났으면 환불 없이 구독권 취소만 수행된다")
    void givenSolePaidActiveSubscriptionPastRefundPeriod_whenCancelSubscription_thenNoRefundAndSubscriptionIsCancelled() {
        Subscription subscription = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(subscription, SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(subscription, "activatedAt", OffsetDateTime.now().minusDays(15));

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        lenient().when(subscriptionRepository.findByMemberId(MEMBER_ID))
                .thenReturn(List.of(subscription));

        subscriptionService.cancelSubscription(MEMBER_ID, SUBSCRIPTION_ID);

        assertNotNull(subscription.getCancelledAt());
        assertEquals(0, subscription.getPayments().size());
        verify(subscriptionRepository).save(subscription);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("유료/유료 구독권 1개/14일 이내이더라도 이미 유효기간이 만료되어 활성 상태(isValid)가 아니면 환불 없이 구독권 취소만 수행된다")
    void givenSolePaidSubscriptionExpiredButWithinRefundWindow_whenCancelSubscription_thenNoRefundAndSubscriptionIsCancelled() {
        Subscription subscription = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(subscription, SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(subscription, "activatedAt", OffsetDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(subscription, "validUntil", OffsetDateTime.now().minusDays(1));

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        lenient().when(subscriptionRepository.findByMemberId(MEMBER_ID))
                .thenReturn(List.of(subscription));

        subscriptionService.cancelSubscription(MEMBER_ID, SUBSCRIPTION_ID);

        assertNotNull(subscription.getCancelledAt());
        assertEquals(0, subscription.getPayments().size());
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
        verify(eventPublisher, never()).publishEvent(any());
    }

    // -------------------------------------------------------------------------
    // suspendActiveSubscriptions (SUB-06: 회원 정지 이벤트 처리 - 활성 구독권 전부 정지.
    // 이미 정지/취소되었거나 아직 활성화되지 않은 구독권은 대상에서 제외해야 한다. 이 필터링이
    // 누락되면 대상이 아닌 구독권에 suspend()가 호출되어 SUB-01의 상태 전이 불변식 위반으로
    // 예외가 발생하며 테스트가 실패한다.)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("활성 구독권이 여러 개이면 모두 정지되고 각각 저장된다")
    void givenMultipleActiveSubscriptions_whenSuspendActiveSubscriptions_thenAllAreSuspendedAndSaved() {
        Subscription activeA = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(activeA, SUBSCRIPTION_ID);
        activeA.activate();

        Subscription activeB = Subscription.create(MEMBER_ID, FREE_PRICE);
        setId(activeB, FREE_SIBLING_SUBSCRIPTION_ID);
        activeB.activate();

        when(subscriptionRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(activeA, activeB));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.suspendActiveSubscriptions(MEMBER_ID);

        assertNotNull(activeA.getSuspendedAt());
        assertNotNull(activeB.getSuspendedAt());
        verify(subscriptionRepository).save(activeA);
        verify(subscriptionRepository).save(activeB);
    }

    @Test
    @DisplayName("이미 정지/취소되었거나 아직 활성화되지 않은 구독권은 건드리지 않고 활성 구독권만 정지한다")
    void givenAlreadySuspendedCancelledOrInactiveSubscriptions_whenSuspendActiveSubscriptions_thenOnlyEligibleSubscriptionIsSuspended() {
        Subscription active = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(active, SUBSCRIPTION_ID);
        active.activate();

        Subscription alreadySuspended = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(alreadySuspended, FREE_SIBLING_SUBSCRIPTION_ID);
        alreadySuspended.activate();
        alreadySuspended.suspend();
        OffsetDateTime suspendedAtBefore = alreadySuspended.getSuspendedAt();

        Subscription alreadyCancelled = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(alreadyCancelled, OTHER_PAID_SUBSCRIPTION_ID);
        alreadyCancelled.activate();
        alreadyCancelled.cancel();

        Subscription notYetActivated = Subscription.create(MEMBER_ID, FREE_PRICE);
        setId(notYetActivated, FOURTH_SUBSCRIPTION_ID);

        when(subscriptionRepository.findByMemberId(MEMBER_ID))
                .thenReturn(List.of(active, alreadySuspended, alreadyCancelled, notYetActivated));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.suspendActiveSubscriptions(MEMBER_ID);

        assertNotNull(active.getSuspendedAt());
        assertEquals(suspendedAtBefore, alreadySuspended.getSuspendedAt());
        assertNull(alreadyCancelled.getSuspendedAt());
        assertNull(notYetActivated.getSuspendedAt());

        verify(subscriptionRepository).save(active);
        verify(subscriptionRepository, never()).save(alreadySuspended);
        verify(subscriptionRepository, never()).save(alreadyCancelled);
        verify(subscriptionRepository, never()).save(notYetActivated);
    }

    @Test
    @DisplayName("대상 활성 구독권이 없으면 아무 것도 저장하지 않는다")
    void givenNoSubscriptionsForMember_whenSuspendActiveSubscriptions_thenNoSaveIsInvoked() {
        when(subscriptionRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of());

        subscriptionService.suspendActiveSubscriptions(MEMBER_ID);

        verify(subscriptionRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // processMemberWithdrawal (SUB-06: 회원 탈퇴 이벤트 처리 - 활성 구독권 전부 정지 + 환불
    // 정책 통과 시 환불. 환불 대상 구독권은 직접 suspend()를 호출하지 않고 Payment(REFUND)
    // 생성/addPayment/저장 후 RefundRequestedEvent만 발행한다(정지는 이후
    // PaymentAdapter.handleRefundRequested()에서 처리됨 - SUB-04에서 이미 구현됨). 나머지
    // 활성 구독권은 각각 suspend() 호출 후 저장한다.)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유일한 유료 구독권이 활성/14일 이내이면 환불이 요청되고 직접 정지되지 않으며, 나머지 활성 구독권은 정지된다")
    void givenSolePaidActiveSubscriptionWithinRefundPeriodAndOtherActiveSubscription_whenProcessMemberWithdrawal_thenRefundRequestedForPaidSubscriptionAndOthersSuspended() {
        Subscription paidSubscription = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(paidSubscription, SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(paidSubscription, "activatedAt", OffsetDateTime.now().minusDays(1));

        Subscription freeSubscription = Subscription.create(MEMBER_ID, FREE_PRICE);
        setId(freeSubscription, FREE_SIBLING_SUBSCRIPTION_ID);
        freeSubscription.activate();

        when(subscriptionRepository.findByMemberId(MEMBER_ID))
                .thenReturn(List.of(paidSubscription, freeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            for (Payment payment : sub.getPayments()) {
                if (ReflectionTestUtils.getField(payment, "id") == null) {
                    setId(payment, PAYMENT_ID);
                }
            }
            return sub;
        });

        subscriptionService.processMemberWithdrawal(MEMBER_ID);

        assertNull(paidSubscription.getSuspendedAt());
        assertEquals(1, paidSubscription.getPayments().size());
        Payment refundPayment = paidSubscription.getPayments().get(0);
        assertEquals(RequestType.REFUND, refundPayment.getRequestType());
        verify(subscriptionRepository).save(paidSubscription);

        assertNotNull(freeSubscription.getSuspendedAt());
        verify(subscriptionRepository).save(freeSubscription);

        ArgumentCaptor<RefundRequestedEvent> eventCaptor = ArgumentCaptor.forClass(RefundRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        RefundRequestedEvent event = eventCaptor.getValue();
        assertEquals(SUBSCRIPTION_ID, event.getSubscriptionId());
        assertEquals(PAYMENT_ID, event.getPaymentId());
    }

    @Test
    @DisplayName("활성 구독권이 모두 무료이면 환불 이벤트 없이 활성 구독권 전부가 정지된다")
    void givenOnlyFreeActiveSubscriptions_whenProcessMemberWithdrawal_thenAllSuspendedWithoutRefundEvent() {
        Subscription freeA = Subscription.create(MEMBER_ID, FREE_PRICE);
        setId(freeA, SUBSCRIPTION_ID);
        freeA.activate();

        Subscription freeB = Subscription.create(MEMBER_ID, FREE_PRICE);
        setId(freeB, FREE_SIBLING_SUBSCRIPTION_ID);
        freeB.activate();

        when(subscriptionRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(freeA, freeB));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.processMemberWithdrawal(MEMBER_ID);

        assertNotNull(freeA.getSuspendedAt());
        assertNotNull(freeB.getSuspendedAt());
        verify(subscriptionRepository).save(freeA);
        verify(subscriptionRepository).save(freeB);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("활성 유료 구독권이 2개 이상이면 환불 이벤트 없이 활성 구독권 전부가 정지된다")
    void givenTwoActivePaidSubscriptions_whenProcessMemberWithdrawal_thenAllSuspendedWithoutRefundEvent() {
        Subscription paidA = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(paidA, SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(paidA, "activatedAt", OffsetDateTime.now().minusDays(1));

        Subscription paidB = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(paidB, OTHER_PAID_SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(paidB, "activatedAt", OffsetDateTime.now().minusDays(1));

        when(subscriptionRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(paidA, paidB));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.processMemberWithdrawal(MEMBER_ID);

        assertNotNull(paidA.getSuspendedAt());
        assertNotNull(paidB.getSuspendedAt());
        verify(subscriptionRepository).save(paidA);
        verify(subscriptionRepository).save(paidB);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("유일한 유료 구독권이더라도 활성화 후 14일이 지났으면 환불 없이 직접 정지된다")
    void givenSolePaidActiveSubscriptionPastRefundPeriod_whenProcessMemberWithdrawal_thenSuspendedDirectlyWithoutRefundEvent() {
        Subscription paidSubscription = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(paidSubscription, SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(paidSubscription, "activatedAt", OffsetDateTime.now().minusDays(15));

        when(subscriptionRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(paidSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.processMemberWithdrawal(MEMBER_ID);

        assertNotNull(paidSubscription.getSuspendedAt());
        assertEquals(0, paidSubscription.getPayments().size());
        verify(subscriptionRepository).save(paidSubscription);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("활성 구독권이 없으면 저장이나 이벤트 발행 없이 아무 것도 하지 않는다")
    void givenNoActiveSubscriptionsForMember_whenProcessMemberWithdrawal_thenNoSaveOrEventInvoked() {
        Subscription alreadySuspended = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(alreadySuspended, SUBSCRIPTION_ID);
        alreadySuspended.activate();
        alreadySuspended.suspend();

        Subscription alreadyCancelled = Subscription.create(MEMBER_ID, FREE_PRICE);
        setId(alreadyCancelled, FREE_SIBLING_SUBSCRIPTION_ID);
        alreadyCancelled.activate();
        alreadyCancelled.cancel();

        when(subscriptionRepository.findByMemberId(MEMBER_ID))
                .thenReturn(List.of(alreadySuspended, alreadyCancelled));

        subscriptionService.processMemberWithdrawal(MEMBER_ID);

        verify(subscriptionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // -------------------------------------------------------------------------
    // reissueExpiringSubscriptions (SUB-07: 매일 0시 배치가 호출하는 자동 재발급 유스케이스.
    // 후보군은 subscriptionRepository.findByActivatedAtIsNotNullAndSuspendedAtIsNullAndCancelledAtIsNull()
    // 로 조회하고, 그중 Subscription.isEligibleForReissue()가 true인 것만
    // subscription.reissue(19_800L)로 재발급하여 저장하고, 재발급된(새) 구독권 기준으로
    // Payment(PAYMENT)를 추가/저장한 뒤 저장된 Payment의 id로 PaymentRequestedEvent를
    // 발행한다. 원본 구독권은 reissue() 호출만으로는 변경되지 않으므로 저장되거나 이벤트의
    // 기준이 되어서는 안 된다.)
    // -------------------------------------------------------------------------

    private Subscription buildCandidate(Long id, OffsetDateTime activatedAt, OffsetDateTime validUntil) {
        Subscription subscription = Subscription.create(MEMBER_ID, PAID_PRICE);
        setId(subscription, id);
        ReflectionTestUtils.setField(subscription, "activatedAt", activatedAt);
        ReflectionTestUtils.setField(subscription, "validUntil", validUntil);
        return subscription;
    }

    @Test
    @DisplayName("만료 임박(2일 이내) 구독권만 각각 19,800원으로 재발급되어 저장되고, 재발급된 구독권 기준으로 결제 요청 이벤트가 발행된다")
    void givenCandidatesWithSomeNearExpiry_whenReissueExpiringSubscriptions_thenOnlyEligibleOnesAreReissuedSavedAndEventsPublished() {
        OffsetDateTime now = OffsetDateTime.now();
        Subscription eligibleA = buildCandidate(ELIGIBLE_SUBSCRIPTION_A_ID, now.minusDays(20), now.plusHours(10));
        Subscription ineligible = buildCandidate(INELIGIBLE_SUBSCRIPTION_ID, now.minusDays(5), now.plusDays(10));
        Subscription eligibleB = buildCandidate(ELIGIBLE_SUBSCRIPTION_B_ID, now.minusDays(25), now.plusHours(30));

        when(subscriptionRepository.findByActivatedAtIsNotNullAndSuspendedAtIsNullAndCancelledAtIsNull())
                .thenReturn(List.of(eligibleA, ineligible, eligibleB));

        AtomicLong nextSubscriptionId = new AtomicLong(FIRST_REISSUED_SUBSCRIPTION_ID);
        AtomicLong nextPaymentId = new AtomicLong(FIRST_REISSUED_PAYMENT_ID);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            if (ReflectionTestUtils.getField(sub, "id") == null) {
                setId(sub, nextSubscriptionId.getAndIncrement());
            }
            for (Payment payment : sub.getPayments()) {
                if (ReflectionTestUtils.getField(payment, "id") == null) {
                    setId(payment, nextPaymentId.getAndIncrement());
                }
            }
            return sub;
        });

        subscriptionService.reissueExpiringSubscriptions();

        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, times(2)).save(subscriptionCaptor.capture());
        List<Subscription> savedSubscriptions = subscriptionCaptor.getAllValues();

        Subscription savedA = savedSubscriptions.get(0);
        assertEquals(REISSUE_PRICE, savedA.getPrice());
        assertEquals(eligibleA.getId(), savedA.getParentId());
        assertEquals(1, savedA.getPayments().size());
        assertEquals(RequestType.PAYMENT, savedA.getPayments().get(0).getRequestType());

        Subscription savedB = savedSubscriptions.get(1);
        assertEquals(REISSUE_PRICE, savedB.getPrice());
        assertEquals(eligibleB.getId(), savedB.getParentId());
        assertEquals(1, savedB.getPayments().size());
        assertEquals(RequestType.PAYMENT, savedB.getPayments().get(0).getRequestType());

        verify(subscriptionRepository, never()).save(eligibleA);
        verify(subscriptionRepository, never()).save(eligibleB);
        verify(subscriptionRepository, never()).save(ineligible);
        assertEquals(0, eligibleA.getPayments().size());
        assertEquals(0, eligibleB.getPayments().size());
        assertEquals(0, ineligible.getPayments().size());

        ArgumentCaptor<PaymentRequestedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRequestedEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<PaymentRequestedEvent> events = eventCaptor.getAllValues();

        assertEquals(savedA.getId(), events.get(0).getSubscriptionId());
        assertEquals(savedA.getPayments().get(0).getId().value(), events.get(0).getPaymentId());
        assertEquals(savedB.getId(), events.get(1).getSubscriptionId());
        assertEquals(savedB.getPayments().get(0).getId().value(), events.get(1).getPaymentId());
    }

    @Test
    @DisplayName("후보군 중 만료 임박이 아닌 구독권만 있으면 재발급되지 않고 저장이나 이벤트 발행이 일어나지 않는다")
    void givenOnlyNonNearExpiryCandidates_whenReissueExpiringSubscriptions_thenNoSaveOrEventInvoked() {
        OffsetDateTime now = OffsetDateTime.now();
        Subscription ineligible = buildCandidate(INELIGIBLE_SUBSCRIPTION_ID, now.minusDays(5), now.plusDays(10));

        when(subscriptionRepository.findByActivatedAtIsNotNullAndSuspendedAtIsNullAndCancelledAtIsNull())
                .thenReturn(List.of(ineligible));

        subscriptionService.reissueExpiringSubscriptions();

        verify(subscriptionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("재발급 대상 후보가 전혀 없으면 아무 것도 저장하거나 발행하지 않는다")
    void givenNoCandidates_whenReissueExpiringSubscriptions_thenNoSaveOrEventInvoked() {
        when(subscriptionRepository.findByActivatedAtIsNotNullAndSuspendedAtIsNullAndCancelledAtIsNull())
                .thenReturn(List.of());

        subscriptionService.reissueExpiringSubscriptions();

        verify(subscriptionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
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
