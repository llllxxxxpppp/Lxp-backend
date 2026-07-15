package com.lcs.lxp.subscription.domain.event;

import com.lcs.lxp.member.event.MemberRegisteredEvent;
import com.lcs.lxp.subscription.application.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 회원가입 이벤트 리스너.
 *
 * <p>Member BC가 발행하는 {@code MemberRegisteredEvent}를 구독하여, 가입한 회원에게
 * 무료 구독권(가격 0원)을 자동으로 발급한다. 무료 구독권은 {@code SubscriptionService}
 * 내부에서 결제가 즉시 성공 처리되어 활성화까지 이어진다.
 */
@Component
public class MemberRegisteredEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemberRegisteredEventListener.class);
    private static final Long FREE_PRICE = 0L;

    private final SubscriptionService subscriptionService;

    public MemberRegisteredEventListener(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @EventListener
    public void handleMemberRegistered(MemberRegisteredEvent event) {
        logEvent("Handling event", event);

        subscriptionService.createSubscription(event.getMemberId(), FREE_PRICE);

        logEvent("Handled event", event);
    }

    private void logEvent(String phase, MemberRegisteredEvent event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{}: type={}, eventId={}, occurredAt={}, memberId={}",
                    phase,
                    event.getClass().getSimpleName(),
                    event.getEventId(),
                    event.getOccurredAt(),
                    event.getMemberId());
        }
    }
}
