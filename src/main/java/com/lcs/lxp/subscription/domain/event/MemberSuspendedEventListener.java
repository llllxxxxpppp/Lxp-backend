package com.lcs.lxp.subscription.domain.event;

import com.lcs.lxp.member.event.MemberSuspendedEvent;
import com.lcs.lxp.subscription.application.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 회원 정지 이벤트 리스너.
 *
 * <p>Member BC가 발행하는 {@code MemberSuspendedEvent}를 구독하여, 정지된 회원의 활성
 * 구독권을 전부 정지시킨다. 실제 정지 로직은 {@code SubscriptionService}에 위임한다.
 */
@Component
public class MemberSuspendedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemberSuspendedEventListener.class);

    private final SubscriptionService subscriptionService;

    public MemberSuspendedEventListener(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @EventListener
    public void handleMemberSuspended(MemberSuspendedEvent event) {
        logEvent("Handling event", event);

        subscriptionService.suspendActiveSubscriptions(event.getMemberId());

        logEvent("Handled event", event);
    }

    private void logEvent(String phase, MemberSuspendedEvent event) {
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
