package com.lcs.lxp.subscription.domain;

import com.lcs.lxp.subscription.domain.model.entity.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SUB-01: 구독권 애그리거트 재설계 검증.
 *
 * <p>상태를 단일 enum이 아닌 활성화/정지/취소 각각의 nullable 일시로 표현하고,
 * 부모 구독권 ID / 구독 시작 일시 / 구독 회차 / reissue() 재발급 체인 / 달력 월 기준
 * 유효기간 계산법을 검증한다. 결제/환불 요청 내역(리스트)은 SUB-02 범위이므로
 * 이 테스트에는 포함하지 않는다.
 */
class SubscriptionTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long PAID_PRICE = 19_800L;

    private Subscription subscription;

    @BeforeEach
    void setUp() {
        subscription = Subscription.create(MEMBER_ID, PAID_PRICE);
    }

    @Test
    @DisplayName("회원 ID와 가격으로 생성하면 부모 구독권 ID는 0, 구독 회차는 1, 구독 시작 일시는 생성 시점으로 설정된다")
    void givenMemberIdAndPrice_whenCreate_thenRootChainDefaultsAreSet() {
        OffsetDateTime beforeCreate = OffsetDateTime.now();

        Subscription created = Subscription.create(MEMBER_ID, PAID_PRICE);

        OffsetDateTime afterCreate = OffsetDateTime.now();

        assertEquals(0L, created.getParentId().longValue());
        assertEquals(1L, created.getGeneration().longValue());
        assertNotNull(created.getSubscriptionStartAt());
        assertFalse(created.getSubscriptionStartAt().isBefore(beforeCreate));
        assertFalse(created.getSubscriptionStartAt().isAfter(afterCreate));
        assertEquals(MEMBER_ID.longValue(), created.getMemberId().longValue());
        assertEquals(PAID_PRICE.longValue(), created.getPrice().longValue());
    }

    @Test
    @DisplayName("구독권을 생성하면 활성화/정지/취소 일시는 모두 NULL이고, 생성 일시는 채워지며 수정 일시는 NULL이다")
    void givenNewSubscription_whenCreate_thenLifecycleTimestampsAreNullAndAuditFieldsAreSet() {
        assertNull(subscription.getActivatedAt());
        assertNull(subscription.getSuspendedAt());
        assertNull(subscription.getCancelledAt());
        assertNotNull(subscription.getCreatedAt());
        assertNull(subscription.getUpdatedAt());
    }

    @Test
    @DisplayName("소유 회원 ID가 NULL이면 구독권을 생성할 수 없다")
    void givenNullMemberId_whenCreate_thenThrowsException() {
        assertThrows(NullPointerException.class, () -> Subscription.create(null, PAID_PRICE));
    }

    @Test
    @DisplayName("가격이 NULL이면 구독권을 생성할 수 없다")
    void givenNullPrice_whenCreate_thenThrowsException() {
        assertThrows(NullPointerException.class, () -> Subscription.create(MEMBER_ID, null));
    }

    @Test
    @DisplayName("구독권 생성 후 활성화를 하더라도 가격과 소유 회원 ID는 변하지 않는다 (불변 필드)")
    void givenCreatedSubscription_whenActivate_thenPriceAndMemberIdRemainUnchanged() {
        subscription.activate();

        assertEquals(PAID_PRICE.longValue(), subscription.getPrice().longValue());
        assertEquals(MEMBER_ID.longValue(), subscription.getMemberId().longValue());
    }

    @Test
    @DisplayName("구독 시작 일시와 구독 회차로 계산한 유효기간은 (시작 일시).plusMonths(회차).plusDays(1).truncatedTo(DAYS) 와 같다")
    void givenSubscriptionStartAtAndGeneration_whenCreate_thenValidUntilMatchesCalculationFormula() {
        OffsetDateTime expected = subscription.getSubscriptionStartAt()
                .plusMonths(subscription.getGeneration())
                .plusDays(1)
                .truncatedTo(ChronoUnit.DAYS);

        assertEquals(expected, subscription.getValidUntil());
    }

    @Test
    @DisplayName("구독 시작일에 회차만큼의 달을 더한 날짜가 존재하지 않으면(예: 12/31 + 2개월) 해당 월의 마지막 날로 계산되어 다음날 00시가 유효기간이 된다")
    void givenStartDateWithNoMatchingDayInTargetMonth_whenReissue_thenValidUntilClampsToLastDayOfMonth() {
        OffsetDateTime endOfDecember = OffsetDateTime.of(2026, 12, 31, 9, 0, 0, 0, ZoneOffset.UTC);
        ReflectionTestUtils.setField(subscription, "subscriptionStartAt", endOfDecember);

        // original generation(1) + 1 = 2, so target month = December + 2 = February(non-leap 2027, 28 days)
        Subscription reissued = subscription.reissue(PAID_PRICE);

        OffsetDateTime expected = OffsetDateTime.of(2027, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(expected, reissued.getValidUntil());
    }

    @Test
    @DisplayName("reissue()를 호출하면 회차는 원본+1, 부모 ID는 원본 id, 구독 시작 일시와 소유 회원 ID는 원본에서 복사, 가격은 새로 입력받은 값으로 저장된다")
    void givenOriginalSubscription_whenReissue_thenChainFieldsAreCopiedAndPriceIsRecalculated() {
        ReflectionTestUtils.setField(subscription, "id", 100L);
        OffsetDateTime originalStartAt = subscription.getSubscriptionStartAt();
        Long newPrice = 9_900L;

        Subscription reissued = subscription.reissue(newPrice);

        assertEquals(2L, reissued.getGeneration().longValue());
        assertEquals(100L, reissued.getParentId().longValue());
        assertEquals(originalStartAt, reissued.getSubscriptionStartAt());
        assertEquals(MEMBER_ID.longValue(), reissued.getMemberId().longValue());
        assertEquals(newPrice.longValue(), reissued.getPrice().longValue());
        OffsetDateTime expectedValidUntil = reissued.getSubscriptionStartAt()
                .plusMonths(reissued.getGeneration())
                .plusDays(1)
                .truncatedTo(ChronoUnit.DAYS);
        assertEquals(expectedValidUntil, reissued.getValidUntil());
    }

    @Test
    @DisplayName("reissue()로 생성된 구독권은 활성화/정지/취소 일시가 NULL이고 수정 일시도 NULL인 기본 상태로 시작한다")
    void givenOriginalSubscription_whenReissue_thenLifecycleFieldsResetToDefaults() {
        subscription.activate();

        Subscription reissued = subscription.reissue(PAID_PRICE);

        assertNull(reissued.getActivatedAt());
        assertNull(reissued.getSuspendedAt());
        assertNull(reissued.getCancelledAt());
        assertNotNull(reissued.getCreatedAt());
        assertNull(reissued.getUpdatedAt());
    }

    @Test
    @DisplayName("activate()를 호출하면 활성화 일시가 채워지고 수정 일시도 갱신되지만 정지/취소 일시는 그대로 NULL이다")
    void givenNewSubscription_whenActivate_thenOnlyActivatedAtAndUpdatedAtAreSet() {
        subscription.activate();

        assertNotNull(subscription.getActivatedAt());
        assertNotNull(subscription.getUpdatedAt());
        assertNull(subscription.getSuspendedAt());
        assertNull(subscription.getCancelledAt());
    }

    @Test
    @DisplayName("suspend()를 호출하면 정지 일시가 채워진다")
    void givenActivatedSubscription_whenSuspend_thenSuspendedAtIsSet() {
        subscription.activate();

        subscription.suspend();

        assertNotNull(subscription.getSuspendedAt());
    }

    @Test
    @DisplayName("cancel()을 호출하면 취소 일시가 채워진다")
    void givenActivatedSubscription_whenCancel_thenCancelledAtIsSet() {
        subscription.activate();

        subscription.cancel();

        assertNotNull(subscription.getCancelledAt());
    }

    @Test
    @DisplayName("활성화되지 않았으면 isValid()는 false이다")
    void givenNotActivatedSubscription_whenIsValid_thenReturnsFalse() {
        assertFalse(subscription.isValid());
    }

    @Test
    @DisplayName("활성화되었고 정지되지 않았고 유효기간이 지나지 않았으면 isValid()는 true이다 (취소 여부 false)")
    void givenActivatedNotSuspendedNotExpiredAndNotCancelled_whenIsValid_thenReturnsTrue() {
        subscription.activate();

        assertTrue(subscription.isValid());
    }

    @Test
    @DisplayName("활성화되었고 정지되지 않았고 유효기간이 지나지 않았으면 취소되었더라도 isValid()는 true이다 (취소 여부는 무관)")
    void givenActivatedNotSuspendedNotExpiredButCancelled_whenIsValid_thenReturnsTrue() {
        subscription.activate();
        subscription.cancel();

        assertTrue(subscription.isValid());
    }

    @Test
    @DisplayName("활성화되었지만 정지되었으면 isValid()는 false이다")
    void givenActivatedAndSuspended_whenIsValid_thenReturnsFalse() {
        subscription.activate();

        subscription.suspend();

        assertFalse(subscription.isValid());
    }

    @Test
    @DisplayName("활성화되었지만 유효기간이 지났으면 isValid()는 false이다")
    void givenActivatedButExpired_whenIsValid_thenReturnsFalse() {
        subscription.activate();
        ReflectionTestUtils.setField(subscription, "validUntil", OffsetDateTime.now().minusDays(1));

        assertFalse(subscription.isValid());
    }

    @Test
    @DisplayName("활성화되지 않았으면 유효기간이 임박해도 isEligibleForReissue()는 false이다")
    void givenNotActivatedSubscription_whenIsEligibleForReissue_thenReturnsFalse() {
        ReflectionTestUtils.setField(subscription, "validUntil", OffsetDateTime.now().plusDays(1));

        assertFalse(subscription.isEligibleForReissue());
    }

    @Test
    @DisplayName("활성화, 정지 아님, 취소 아님이고 유효기간 만료 2일 이내이면 isEligibleForReissue()는 true이다")
    void givenActivatedNotSuspendedNotCancelledAndNearExpiry_whenIsEligibleForReissue_thenReturnsTrue() {
        subscription.activate();
        ReflectionTestUtils.setField(subscription, "validUntil", OffsetDateTime.now().plusDays(1));

        assertTrue(subscription.isEligibleForReissue());
    }

    @Test
    @DisplayName("활성화, 정지 아님, 취소 아님이더라도 유효기간까지 아직 여유가 있으면 isEligibleForReissue()는 false이다")
    void givenActivatedButFarFromExpiry_whenIsEligibleForReissue_thenReturnsFalse() {
        subscription.activate();
        ReflectionTestUtils.setField(subscription, "validUntil", OffsetDateTime.now().plusDays(10));

        assertFalse(subscription.isEligibleForReissue());
    }

    @Test
    @DisplayName("활성화, 정지 아님, 취소 아님이더라도 유효기간이 이미 한참 지났으면 isEligibleForReissue()는 false이다")
    void givenActivatedButAlreadyExpiredLongAgo_whenIsEligibleForReissue_thenReturnsFalse() {
        subscription.activate();
        ReflectionTestUtils.setField(subscription, "validUntil", OffsetDateTime.now().minusDays(100));

        assertFalse(subscription.isEligibleForReissue());
    }

    @Test
    @DisplayName("정지된 구독권은 유효기간이 임박해도 isEligibleForReissue()는 false이다")
    void givenSuspendedSubscriptionNearExpiry_whenIsEligibleForReissue_thenReturnsFalse() {
        subscription.activate();
        subscription.suspend();
        ReflectionTestUtils.setField(subscription, "validUntil", OffsetDateTime.now().plusDays(1));

        assertFalse(subscription.isEligibleForReissue());
    }

    @Test
    @DisplayName("취소된 구독권은 유효기간이 임박해도 isEligibleForReissue()는 false이다")
    void givenCancelledSubscriptionNearExpiry_whenIsEligibleForReissue_thenReturnsFalse() {
        subscription.activate();
        subscription.cancel();
        ReflectionTestUtils.setField(subscription, "validUntil", OffsetDateTime.now().plusDays(1));

        assertFalse(subscription.isEligibleForReissue());
    }

    // -------------------------------------------------------------------------
    // isWithinRefundPeriod (SUB-04: 환불 조건 복원)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("활성화되지 않은(activatedAt이 NULL인) 구독권은 isWithinRefundPeriod()가 false이다")
    void givenNotActivatedSubscription_whenIsWithinRefundPeriod_thenReturnsFalse() {
        assertNull(subscription.getActivatedAt());

        assertFalse(subscription.isWithinRefundPeriod());
    }

    @Test
    @DisplayName("활성화된 지 14일이 지나지 않았으면 isWithinRefundPeriod()는 true이다")
    void givenActivatedWithin14Days_whenIsWithinRefundPeriod_thenReturnsTrue() {
        ReflectionTestUtils.setField(subscription, "activatedAt", OffsetDateTime.now().minusDays(1));

        assertTrue(subscription.isWithinRefundPeriod());
    }

    @Test
    @DisplayName("활성화된 지 14일이 지났으면 isWithinRefundPeriod()는 false이다")
    void givenActivatedMoreThan14DaysAgo_whenIsWithinRefundPeriod_thenReturnsFalse() {
        ReflectionTestUtils.setField(subscription, "activatedAt", OffsetDateTime.now().minusDays(15));

        assertFalse(subscription.isWithinRefundPeriod());
    }
}
