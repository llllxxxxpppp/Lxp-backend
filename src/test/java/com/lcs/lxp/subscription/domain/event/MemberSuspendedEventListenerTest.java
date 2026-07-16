package com.lcs.lxp.subscription.domain.event;

import com.lcs.lxp.member.event.MemberSuspendedEvent;
import com.lcs.lxp.subscription.application.service.SubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * SUB-06: 회원 정지 이벤트를 구독해 활성 구독권을 전부 정지시키는 리스너 검증.
 *
 * <p>{@code MemberSuspendedEventListener}는 {@code @EventListener}로
 * {@code MemberSuspendedEvent}(Member BC 발행)를 구독하여, 이벤트의 회원 id로
 * {@code SubscriptionService#suspendActiveSubscriptions}를 호출한다. 실제 정지 로직 자체는
 * {@code SubscriptionServiceTest}에서 검증되므로, 이 테스트는 리스너가 올바른 인자로 서비스
 * 메서드를 위임 호출하는지만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberSuspendedEventListenerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 42L;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private MemberSuspendedEventListener listener;

    @Test
    @DisplayName("회원 정지 이벤트를 수신하면 이벤트의 회원 id로 활성 구독권 정지를 요청한다")
    void givenMemberSuspendedEvent_whenHandleMemberSuspended_thenSuspendsActiveSubscriptionsForMember() {
        MemberSuspendedEvent event = new MemberSuspendedEvent(MEMBER_ID);

        listener.handleMemberSuspended(event);

        verify(subscriptionService).suspendActiveSubscriptions(MEMBER_ID);
        verifyNoMoreInteractions(subscriptionService);
    }

    @Test
    @DisplayName("다른 회원의 정지 이벤트를 수신하면 그 회원 id로 활성 구독권 정지를 요청한다")
    void givenAnotherMemberSuspendedEvent_whenHandleMemberSuspended_thenSuspendsActiveSubscriptionsForThatMember() {
        MemberSuspendedEvent event = new MemberSuspendedEvent(OTHER_MEMBER_ID);

        listener.handleMemberSuspended(event);

        verify(subscriptionService).suspendActiveSubscriptions(OTHER_MEMBER_ID);
        verifyNoMoreInteractions(subscriptionService);
    }
}
