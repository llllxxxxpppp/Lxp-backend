package com.lcs.lxp.subscription.domain.event;

import com.lcs.lxp.member.event.MemberWithdrawnEvent;
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
 * SUB-06: 회원 탈퇴 이벤트를 구독해 구독권 정지/환불 처리를 위임하는 리스너 검증.
 *
 * <p>{@code MemberWithdrawnEventListener}는 {@code @EventListener}로
 * {@code MemberWithdrawnEvent}(Member BC 발행)를 구독하여, 이벤트의 회원 id로
 * {@code SubscriptionService#processMemberWithdrawal}을 호출한다. 실제 정지/환불 로직
 * 자체는 {@code SubscriptionServiceTest}에서 검증되므로, 이 테스트는 리스너가 올바른 인자로
 * 서비스 메서드를 위임 호출하는지만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberWithdrawnEventListenerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 42L;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private MemberWithdrawnEventListener listener;

    @Test
    @DisplayName("회원 탈퇴 이벤트를 수신하면 이벤트의 회원 id로 탈퇴 처리(정지/환불)를 요청한다")
    void givenMemberWithdrawnEvent_whenHandleMemberWithdrawn_thenProcessesWithdrawalForMember() {
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(MEMBER_ID);

        listener.handleMemberWithdrawn(event);

        verify(subscriptionService).processMemberWithdrawal(MEMBER_ID);
        verifyNoMoreInteractions(subscriptionService);
    }

    @Test
    @DisplayName("다른 회원의 탈퇴 이벤트를 수신하면 그 회원 id로 탈퇴 처리를 요청한다")
    void givenAnotherMemberWithdrawnEvent_whenHandleMemberWithdrawn_thenProcessesWithdrawalForThatMember() {
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(OTHER_MEMBER_ID);

        listener.handleMemberWithdrawn(event);

        verify(subscriptionService).processMemberWithdrawal(OTHER_MEMBER_ID);
        verifyNoMoreInteractions(subscriptionService);
    }
}
