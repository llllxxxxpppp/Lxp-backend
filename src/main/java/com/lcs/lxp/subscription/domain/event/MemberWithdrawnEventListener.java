package com.lcs.lxp.subscription.domain.event;

import com.lcs.lxp.member.event.MemberWithdrawnEvent;
import com.lcs.lxp.subscription.application.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 회원 탈퇴 이벤트 리스너.
 *
 * <p>Member BC가 발행하는 {@code MemberWithdrawnEvent}를 구독하여, 탈퇴한 회원의 구독권을
 * 정지·환불 처리한다. 실제 정지/환불 로직은 {@code SubscriptionService}에 위임한다.
 */
@Component
public class MemberWithdrawnEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemberWithdrawnEventListener.class);

    private final SubscriptionService subscriptionService;

    public MemberWithdrawnEventListener(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @EventListener
    public void handleMemberWithdrawn(MemberWithdrawnEvent event) {
        logEvent("Handling event", event);

        subscriptionService.processMemberWithdrawal(event.getMemberId());

        logEvent("Handled event", event);
    }

    private void logEvent(String phase, MemberWithdrawnEvent event) {
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
