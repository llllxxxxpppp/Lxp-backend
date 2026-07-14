package com.lcs.lxp.subscription.application.service;

import com.lcs.lxp.subscription.application.dto.response.SubscriptionResponse;
import com.lcs.lxp.subscription.domain.event.PaymentRequestedEvent;
import com.lcs.lxp.subscription.domain.exception.SubscriptionException;
import com.lcs.lxp.subscription.domain.model.entity.Payment;
import com.lcs.lxp.subscription.domain.model.entity.Subscription;
import com.lcs.lxp.subscription.domain.model.vo.RequestType;
import com.lcs.lxp.subscription.domain.model.vo.ResponseResult;
import com.lcs.lxp.subscription.domain.repository.SubscriptionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구독권 생성/취소/조회 유스케이스.
 *
 * <p>결제/환불은 더 이상 직접 호출하지 않고 이벤트({@code PaymentRequestedEvent})를
 * 발행하는 방식으로 전환되었다. 실제 PG 호출과 결과 반영은 {@code PaymentAdapter}의
 * 이벤트 리스너 책임이다.
 */
@Service
@Transactional
public class SubscriptionService {

    private static final long FREE_PRICE = 0L;

    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
    }

    public SubscriptionResponse createSubscription(Long memberId, Long price) {
        Subscription subscription = Subscription.create(memberId, price);
        Payment payment = Payment.create(RequestType.PAYMENT);
        subscription.addPayment(payment);

        if (price == FREE_PRICE) {
            payment.markRequested();
            payment.markResponded(ResponseResult.SUCCESS);
            subscription.activate();
            subscription = subscriptionRepository.save(subscription);
        } else {
            subscription = subscriptionRepository.save(subscription);
            Payment savedPayment = subscription.getPayments().get(0);
            eventPublisher.publishEvent(
                    new PaymentRequestedEvent(subscription.getId(), savedPayment.getId().value()));
        }

        return SubscriptionResponse.from(subscription);
    }

    public void cancelSubscription(Long memberId, Long subscriptionId) {
        Subscription subscription = findSubscription(subscriptionId);

        if (!subscription.getMemberId().equals(memberId)) {
            throw new SubscriptionException("본인의 구독권만 취소할 수 있습니다.");
        }

        subscription.cancel();
        subscriptionRepository.save(subscription);
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
