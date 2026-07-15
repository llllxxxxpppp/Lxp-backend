package com.lcs.lxp.subscription.domain;

import com.lcs.lxp.member.event.MemberRegisteredEvent;
import com.lcs.lxp.subscription.application.service.SubscriptionService;
import com.lcs.lxp.subscription.domain.event.MemberRegisteredEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * SUB-05: 회원가입 이벤트를 구독해 무료 구독권을 자동 발급하는 리스너 검증.
 *
 * <p>{@code MemberRegisteredEventListener}는 {@code @EventListener}로
 * {@code MemberRegisteredEvent}(Member BC 발행)를 구독하여, 이벤트의 회원 id와
 * 가격 0원(무료)으로 {@code SubscriptionService#createSubscription}을 호출한다.
 * 무료 구독권의 즉시 활성화 로직 자체는 이미 {@code SubscriptionService}에서 검증되었으므로,
 * 이 테스트는 리스너가 올바른 인자로 서비스 메서드를 호출하는지만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberRegisteredEventListenerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 42L;
    private static final Long FREE_PRICE = 0L;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private MemberRegisteredEventListener listener;

    @Test
    @DisplayName("회원가입 이벤트를 수신하면 이벤트의 회원 id와 무료 가격(0원)으로 구독권 생성을 요청한다")
    void givenMemberRegisteredEvent_whenHandleMemberRegistered_thenCreatesFreeSubscriptionForMember() {
        MemberRegisteredEvent event = new MemberRegisteredEvent(MEMBER_ID);

        listener.handleMemberRegistered(event);

        verify(subscriptionService).createSubscription(MEMBER_ID, FREE_PRICE);
        verifyNoMoreInteractions(subscriptionService);
    }

    @Test
    @DisplayName("다른 회원의 가입 이벤트를 수신하면 그 회원 id로 구독권 생성을 요청한다")
    void givenAnotherMemberRegisteredEvent_whenHandleMemberRegistered_thenCreatesFreeSubscriptionForThatMember() {
        MemberRegisteredEvent event = new MemberRegisteredEvent(OTHER_MEMBER_ID);

        listener.handleMemberRegistered(event);

        verify(subscriptionService).createSubscription(OTHER_MEMBER_ID, FREE_PRICE);
        verifyNoMoreInteractions(subscriptionService);
    }
}
