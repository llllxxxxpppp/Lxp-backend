package com.lcs.lxp.subscription.domain.model.entity;

import com.lcs.lxp.subscription.domain.exception.SubscriptionException;
import com.lcs.lxp.subscription.domain.model.vo.PaymentFailureResponse;
import com.lcs.lxp.subscription.domain.model.vo.PaymentSuccessResponse;
import com.lcs.lxp.subscription.domain.model.vo.SubscriptionId;
import com.lcs.lxp.subscription.domain.model.vo.SubscriptionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    private static final int SUBSCRIPTION_DURATION_DAYS = 31;
    private static final int REFUND_PERIOD_DAYS = 14;
    private static final String INACTIVE_REQUIRED = "비활성 상태에서만 가능합니다.";
    private static final String ACTIVE_REQUIRED = "활성 상태에서만 가능합니다.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Column
    private OffsetDateTime activatedAt;

    @OneToOne(mappedBy = "subscription", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Payment payment;

    protected Subscription() {}

    public static Subscription create(Long memberId) {
        Objects.requireNonNull(memberId, "memberId는 null일 수 없습니다.");
        Subscription sub = new Subscription();
        sub.memberId = memberId;
        sub.status = SubscriptionStatus.INACTIVE;
        sub.createdAt = OffsetDateTime.now();
        sub.expiresAt = sub.createdAt.plusDays(SUBSCRIPTION_DURATION_DAYS);
        return sub;
    }

    public void activate() {
        if (status != SubscriptionStatus.INACTIVE) {
            throw new SubscriptionException(INACTIVE_REQUIRED);
        }
        status = SubscriptionStatus.ACTIVE;
        activatedAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    public void activateByPayment(PaymentSuccessResponse response) {
        Objects.requireNonNull(response, "결제 성공 응답은 null일 수 없습니다.");
        if (status != SubscriptionStatus.INACTIVE) {
            throw new SubscriptionException(INACTIVE_REQUIRED);
        }
        payment.handleSuccess(response);
        status = SubscriptionStatus.ACTIVE;
        activatedAt = response.approvedAt();
        updatedAt = OffsetDateTime.now();
    }

    public void markPaymentFailed(PaymentFailureResponse response) {
        Objects.requireNonNull(response, "결제 실패 응답은 null일 수 없습니다.");
        if (status != SubscriptionStatus.INACTIVE) {
            throw new SubscriptionException(INACTIVE_REQUIRED);
        }
        payment.handleFailure(response);
        status = SubscriptionStatus.PAYMENT_FAILED;
        updatedAt = OffsetDateTime.now();
    }

    public void cancel() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionException(ACTIVE_REQUIRED);
        }
        status = SubscriptionStatus.CANCELLED;
        updatedAt = OffsetDateTime.now();
    }

    public void suspend() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionException(ACTIVE_REQUIRED);
        }
        status = SubscriptionStatus.SUSPENDED;
        updatedAt = OffsetDateTime.now();
    }

    public void discard() {
        status = SubscriptionStatus.DISCARDED;
        updatedAt = OffsetDateTime.now();
    }

    public boolean isValid() {
        return status == SubscriptionStatus.ACTIVE && OffsetDateTime.now().isBefore(expiresAt);
    }

    public boolean isWithinRefundPeriod() {
        if (activatedAt == null) {
            return false;
        }
        return OffsetDateTime.now().isBefore(activatedAt.plusDays(REFUND_PERIOD_DAYS));
    }

    public SubscriptionId getId() {
        return new SubscriptionId(id);
    }

    public Long getMemberId() {
        return memberId;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public Payment getPayment() {
        return payment;
    }

    public void assignPayment(Payment payment) {
        this.payment = payment;
    }
}
