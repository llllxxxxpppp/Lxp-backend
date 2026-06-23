package com.lcs.lxp.subscription.application.service;

import com.lcs.lxp.subscription.application.dto.response.SubscriptionResponse;
import com.lcs.lxp.subscription.domain.exception.SubscriptionException;
import com.lcs.lxp.subscription.domain.model.entity.Payment;
import com.lcs.lxp.subscription.domain.model.entity.Subscription;
import com.lcs.lxp.subscription.domain.model.vo.PaymentInfo;
import com.lcs.lxp.subscription.domain.model.vo.SubscriptionStatus;
import com.lcs.lxp.subscription.domain.repository.SubscriptionRepository;
import com.lcs.lxp.subscription.infrastructure.PaymentAdapter;
import com.lcs.lxp.subscription.infrastructure.PaymentResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubscriptionService {

    private static final int MONTHLY_SUBSCRIPTION_PRICE = 9900;

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentAdapter paymentAdapter;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            PaymentAdapter paymentAdapter) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentAdapter = paymentAdapter;
    }

    public SubscriptionResponse createSubscription(Long memberId) {
        boolean hasExisting = subscriptionRepository.existsByMemberId(memberId);
        int amount = hasExisting ? MONTHLY_SUBSCRIPTION_PRICE : 0;

        Subscription subscription = Subscription.create(memberId);
        PaymentInfo paymentInfo = new PaymentInfo(UUID.randomUUID().toString(), amount);
        Payment payment = Payment.create(subscription, paymentInfo);
        subscription.assignPayment(payment);
        subscriptionRepository.save(subscription);

        if (payment.isFree()) {
            subscription.activate();
        } else {
            PaymentResult result = paymentAdapter.requestPayment(payment);
            if (result.success()) {
                subscription.activateByPayment(result.successResponse());
            } else {
                subscription.markPaymentFailed(result.failureResponse());
            }
        }
        subscriptionRepository.save(subscription);

        return SubscriptionResponse.from(subscription);
    }

    public void cancelSubscription(Long memberId, Long subscriptionId) {
        Subscription subscription = findSubscription(subscriptionId);

        if (!subscription.getMemberId().equals(memberId)) {
            throw new SubscriptionException("본인의 구독권만 취소할 수 있습니다.");
        }

        Payment payment = subscription.getPayment();
        boolean shouldRefund = payment != null
                && !payment.isFree()
                && subscription.isWithinRefundPeriod();

        if (shouldRefund) {
            paymentAdapter.requestRefund(payment);
        }

        subscription.cancel();
        subscriptionRepository.save(subscription);
    }

    public void suspendSubscription(Long memberId) {
        subscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE)
                .ifPresent(subscription -> {
                    subscription.suspend();
                    subscriptionRepository.save(subscription);
                });
    }

    public void reissueExpiring() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime tomorrow = now.plusDays(1);
        List<Subscription> expiring = subscriptionRepository
                .findByStatusAndExpiresAtBetween(SubscriptionStatus.ACTIVE, now, tomorrow);

        for (Subscription old : expiring) {
            old.discard();
            subscriptionRepository.save(old);

            Subscription newSubscription = Subscription.create(old.getMemberId());
            PaymentInfo paymentInfo = new PaymentInfo(UUID.randomUUID().toString(), MONTHLY_SUBSCRIPTION_PRICE);
            Payment payment = Payment.create(newSubscription, paymentInfo);
            newSubscription.assignPayment(payment);
            subscriptionRepository.save(newSubscription);

            PaymentResult result = paymentAdapter.requestPayment(payment);
            if (result.success()) {
                newSubscription.activateByPayment(result.successResponse());
            } else {
                newSubscription.markPaymentFailed(result.failureResponse());
            }
            subscriptionRepository.save(newSubscription);
        }
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionInfo(Long subscriptionId) {
        return SubscriptionResponse.from(findSubscription(subscriptionId));
    }

    private Subscription findSubscription(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionException("구독권을 찾을 수 없습니다."));
    }
}
