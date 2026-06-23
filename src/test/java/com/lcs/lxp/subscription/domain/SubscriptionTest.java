package com.lcs.lxp.subscription.domain;

import com.lcs.lxp.subscription.domain.exception.SubscriptionException;
import com.lcs.lxp.subscription.domain.model.entity.Subscription;
import com.lcs.lxp.subscription.domain.model.vo.PaymentId;
import com.lcs.lxp.subscription.domain.model.vo.PaymentSuccessResponse;
import com.lcs.lxp.subscription.domain.model.vo.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionTest {

    private Subscription subscription;

    @BeforeEach
    void setUp() {
        subscription = Subscription.create(1L);
    }

    @Test
    @DisplayName("구독권을 생성하면 비활성 상태이고 31일 뒤에 만료된다")
    void givenMemberId_whenCreate_thenStatusIsInactiveAndExpiresIn31Days() {
        assertEquals(SubscriptionStatus.INACTIVE, subscription.getStatus());
        assertNotNull(subscription.getCreatedAt());
        assertNull(subscription.getActivatedAt());
        assertTrue(subscription.getExpiresAt().isAfter(subscription.getCreatedAt().plusDays(30)));
    }

    @Test
    @DisplayName("비활성 구독권을 활성화하면 상태가 ACTIVE가 되고 activatedAt이 설정된다")
    void givenInactiveSubscription_whenActivate_thenStatusIsActiveAndActivatedAtIsSet() {
        subscription.activate();

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertNotNull(subscription.getActivatedAt());
        assertNotNull(subscription.getUpdatedAt());
    }

    @Test
    @DisplayName("비활성 상태가 아닌 구독권을 활성화하면 예외가 발생한다")
    void givenNonInactiveSubscription_whenActivate_thenThrowsException() {
        subscription.activate();

        assertThrows(SubscriptionException.class, () -> subscription.activate());
    }

    @Test
    @DisplayName("결제 성공 응답으로 활성화하면 상태가 ACTIVE가 되고 activatedAt에 승인 일시가 설정된다")
    void givenInactiveSubscription_whenActivateByPayment_thenStatusIsActiveAndActivatedAtIsSet() {
        OffsetDateTime approvedAt = OffsetDateTime.now();
        PaymentSuccessResponse response = new PaymentSuccessResponse(new PaymentId(1L), approvedAt);

        subscription.activateByPayment(response);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(approvedAt, subscription.getActivatedAt());
    }

    @Test
    @DisplayName("비활성 상태가 아닌 구독권에 결제 성공 처리하면 예외가 발생한다")
    void givenNonInactiveSubscription_whenActivateByPayment_thenThrowsException() {
        subscription.activate();
        PaymentSuccessResponse response = new PaymentSuccessResponse(new PaymentId(1L), OffsetDateTime.now());

        assertThrows(SubscriptionException.class, () -> subscription.activateByPayment(response));
    }

    @Test
    @DisplayName("결제 실패 처리 시 상태가 PAYMENT_FAILED가 된다")
    void givenInactiveSubscription_whenMarkPaymentFailed_thenStatusIsPaymentFailed() {
        subscription.markPaymentFailed();

        assertEquals(SubscriptionStatus.PAYMENT_FAILED, subscription.getStatus());
        assertNotNull(subscription.getUpdatedAt());
    }

    @Test
    @DisplayName("비활성 상태가 아닌 구독권에 결제 실패 처리하면 예외가 발생한다")
    void givenNonInactiveSubscription_whenMarkPaymentFailed_thenThrowsException() {
        subscription.activate();

        assertThrows(SubscriptionException.class, () -> subscription.markPaymentFailed());
    }

    @Test
    @DisplayName("활성 구독권을 취소하면 상태가 CANCELLED가 된다")
    void givenActiveSubscription_whenCancel_thenStatusIsCancelled() {
        subscription.activate();

        subscription.cancel();

        assertEquals(SubscriptionStatus.CANCELLED, subscription.getStatus());
    }

    @Test
    @DisplayName("활성 상태가 아닌 구독권을 취소하면 예외가 발생한다")
    void givenNonActiveSubscription_whenCancel_thenThrowsException() {
        assertThrows(SubscriptionException.class, () -> subscription.cancel());
    }

    @Test
    @DisplayName("활성 구독권을 정지하면 상태가 SUSPENDED가 된다")
    void givenActiveSubscription_whenSuspend_thenStatusIsSuspended() {
        subscription.activate();

        subscription.suspend();

        assertEquals(SubscriptionStatus.SUSPENDED, subscription.getStatus());
    }

    @Test
    @DisplayName("활성 상태가 아닌 구독권을 정지하면 예외가 발생한다")
    void givenNonActiveSubscription_whenSuspend_thenThrowsException() {
        assertThrows(SubscriptionException.class, () -> subscription.suspend());
    }

    @Test
    @DisplayName("구독권을 폐기하면 상태가 DISCARDED가 된다")
    void givenActiveSubscription_whenDiscard_thenStatusIsDiscarded() {
        subscription.activate();

        subscription.discard();

        assertEquals(SubscriptionStatus.DISCARDED, subscription.getStatus());
    }

    @Test
    @DisplayName("활성 상태이고 만료 전인 구독권은 유효하다")
    void givenActiveSubscriptionBeforeExpiry_whenIsValid_thenReturnsTrue() {
        subscription.activate();

        assertTrue(subscription.isValid());
    }

    @Test
    @DisplayName("활성 상태이지만 만료된 구독권은 유효하지 않다")
    void givenActiveSubscriptionAfterExpiry_whenIsValid_thenReturnsFalse() {
        subscription.activate();
        ReflectionTestUtils.setField(subscription, "expiresAt", OffsetDateTime.now().minusDays(1));

        assertFalse(subscription.isValid());
    }

    @Test
    @DisplayName("취소된 구독권은 유효하지 않다")
    void givenCancelledSubscription_whenIsValid_thenReturnsFalse() {
        subscription.activate();
        subscription.cancel();

        assertFalse(subscription.isValid());
    }

    @Test
    @DisplayName("활성화 후 14일 이내이면 환불 기간 내이다")
    void givenActivatedWithin14Days_whenIsWithinRefundPeriod_thenReturnsTrue() {
        subscription.activate();

        assertTrue(subscription.isWithinRefundPeriod());
    }

    @Test
    @DisplayName("활성화 후 14일이 초과되면 환불 기간이 아니다")
    void givenActivatedOver14DaysAgo_whenIsWithinRefundPeriod_thenReturnsFalse() {
        subscription.activate();
        ReflectionTestUtils.setField(subscription, "activatedAt", OffsetDateTime.now().minusDays(15));

        assertFalse(subscription.isWithinRefundPeriod());
    }

    @Test
    @DisplayName("활성화되지 않은 구독권은 환불 기간이 아니다")
    void givenNotActivatedSubscription_whenIsWithinRefundPeriod_thenReturnsFalse() {
        assertFalse(subscription.isWithinRefundPeriod());
    }
}
