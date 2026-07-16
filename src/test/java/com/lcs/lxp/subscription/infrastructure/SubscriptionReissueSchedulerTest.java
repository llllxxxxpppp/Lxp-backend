package com.lcs.lxp.subscription.infrastructure;

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
 * SUB-07: 만료 임박 구독권 자동 재발급 배치 스케줄러 검증.
 *
 * <p>{@code SubscriptionReissueScheduler}는 {@code @Scheduled(cron = "0 0 0 * * *")}로
 * 매일 0시에 {@code SubscriptionService.reissueExpiringSubscriptions()}를 호출하는
 * 위임 책임만 갖는다. 이 테스트는 스프링 스케줄링 컨텍스트를 띄우지 않고, 스케줄러의
 * 공개 메서드를 직접 호출하여 SubscriptionService로의 위임 여부만 검증한다(실제 cron
 * 트리거 자체는 이 테스트의 범위가 아니다).
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionReissueSchedulerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionReissueScheduler subscriptionReissueScheduler;

    @Test
    @DisplayName("스케줄러가 실행되면 SubscriptionService의 만료 임박 구독권 재발급 메서드를 위임 호출한다")
    void givenScheduledInvocation_whenReissueExpiringSubscriptions_thenDelegatesToSubscriptionService() {
        subscriptionReissueScheduler.reissueExpiringSubscriptions();

        verify(subscriptionService).reissueExpiringSubscriptions();
        verifyNoMoreInteractions(subscriptionService);
    }
}
