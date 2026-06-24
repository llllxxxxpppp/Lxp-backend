package com.lcs.lxp.subscription.domain.model.entity;

import com.lcs.lxp.subscription.domain.exception.SubscriptionException;
import com.lcs.lxp.subscription.domain.model.vo.PaymentFailureResponse;
import com.lcs.lxp.subscription.domain.model.vo.PaymentId;
import com.lcs.lxp.subscription.domain.model.vo.PaymentInfo;
import com.lcs.lxp.subscription.domain.model.vo.PaymentStatus;
import com.lcs.lxp.subscription.domain.model.vo.PaymentSuccessResponse;
import com.lcs.lxp.subscription.domain.model.vo.RefundFailureResponse;
import com.lcs.lxp.subscription.domain.model.vo.RefundInfo;
import com.lcs.lxp.subscription.domain.model.vo.RefundSuccessResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "payments")
public class Payment {

    private static final String ALREADY_PROCESSED = "이미 처리된 결제입니다.";
    private static final String ALREADY_REFUNDED = "이미 환불된 결제입니다.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Embedded
    private PaymentInfo info;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    protected Payment() {}

    public static Payment create(Subscription subscription, PaymentInfo info) {
        Objects.requireNonNull(subscription, "subscription은 null일 수 없습니다.");
        Payment payment = new Payment();
        payment.subscription = subscription;
        payment.info = info;
        payment.status = PaymentStatus.PAYMENT_NOT_REQUESTED;
        payment.createdAt = OffsetDateTime.now();
        return payment;
    }

    public PaymentId getId() {
        return new PaymentId(id);
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return info.getIdempotencyKey();
    }

    public int getAmount() {
        return info.getAmount();
    }

    public boolean isFree() {
        return info.isFree();
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void requestPayment() {
        status = PaymentStatus.PAYMENT_REQUESTED;
        updatedAt = OffsetDateTime.now();
    }

    public void handleSuccess(PaymentSuccessResponse response) {
        Objects.requireNonNull(response, "결제 성공 응답은 null일 수 없습니다.");
        if (status == PaymentStatus.PAYMENT_SUCCESS || status == PaymentStatus.PAYMENT_FAILED) {
            throw new SubscriptionException(ALREADY_PROCESSED);
        }
        status = PaymentStatus.PAYMENT_SUCCESS;
        updatedAt = OffsetDateTime.now();
    }

    public void handleFailure(PaymentFailureResponse response) {
        Objects.requireNonNull(response, "결제 실패 응답은 null일 수 없습니다.");
        if (status == PaymentStatus.PAYMENT_SUCCESS || status == PaymentStatus.PAYMENT_FAILED) {
            throw new SubscriptionException(ALREADY_PROCESSED);
        }
        status = PaymentStatus.PAYMENT_FAILED;
        updatedAt = OffsetDateTime.now();
    }

    public void requestRefund(RefundInfo refundInfo) {
        Objects.requireNonNull(refundInfo, "환불 정보는 null일 수 없습니다.");
        if (status != PaymentStatus.PAYMENT_SUCCESS) {
            throw new SubscriptionException("결제 성공 상태에서만 환불을 요청할 수 있습니다.");
        }
        status = PaymentStatus.REFUND_REQUESTED;
        updatedAt = OffsetDateTime.now();
    }

    public void handleRefundSuccess(RefundSuccessResponse response) {
        Objects.requireNonNull(response, "환불 성공 응답은 null일 수 없습니다.");
        if (status == PaymentStatus.REFUND_SUCCESS) {
            throw new SubscriptionException(ALREADY_REFUNDED);
        }
        status = PaymentStatus.REFUND_SUCCESS;
        updatedAt = OffsetDateTime.now();
    }

    public void handleRefundFailure(RefundFailureResponse response) {
        Objects.requireNonNull(response, "환불 실패 응답은 null일 수 없습니다.");
        if (status == PaymentStatus.REFUND_SUCCESS) {
            throw new SubscriptionException(ALREADY_REFUNDED);
        }
        status = PaymentStatus.REFUND_FAILED;
        updatedAt = OffsetDateTime.now();
    }
}
