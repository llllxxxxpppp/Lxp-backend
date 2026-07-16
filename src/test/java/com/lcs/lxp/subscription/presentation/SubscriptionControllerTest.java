package com.lcs.lxp.subscription.presentation;

import com.lcs.lxp.security.jwt.JwtTokenProvider;
import com.lcs.lxp.security.principal.CustomUserPrincipal;
import com.lcs.lxp.security.refresh.RefreshService;
import com.lcs.lxp.subscription.application.dto.response.SubscriptionResponse;
import com.lcs.lxp.subscription.application.service.SubscriptionService;
import com.lcs.lxp.subscription.domain.exception.SubscriptionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SUB-04: 구독권 조회/취소 API 정합성 조정 검증.
 *
 * <p>도메인 문서상 근거가 없는 수동 생성({@code POST /api/subscriptions})과, 대응 서비스
 * 메서드가 이미 제거된 재발급({@code POST /api/subscriptions/reissue}) 엔드포인트는
 * 컨트롤러에서 제거되었으므로 이 테스트에서 다루지 않는다. 컨트롤러에는 조회와 취소
 * 엔드포인트만 남는다.
 */
@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RefreshService refreshService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    private SubscriptionResponse sampleResponse() {
        OffsetDateTime now = OffsetDateTime.now();
        return new SubscriptionResponse(1L, 1L, 0L, 1L, now, now.plusDays(31), now, null, null, now);
    }

    /**
     * {@code @WithMockUser}가 생성하는 principal은 일반 스프링 시큐리티 {@code User}(UserDetails)로
     * {@code CustomUserPrincipal}이 아니므로, 컨트롤러가 {@link CustomUserPrincipal}로 캐스팅하여
     * ID를 꺼내는 실제 흐름({@code getUserId()})을 검증하려면 {@code CustomUserPrincipal}을 담은
     * 인증 객체를 직접 주입해야 한다.
     */
    private Authentication memberAuthentication(long memberId) {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                memberId, "member" + memberId + "@example.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")),
                false
        );
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    /**
     * {@code CustomUserPrincipal}이 아닌 일반 스프링 시큐리티 {@code UserDetails}({@code User}) 기반
     * 인증 객체. 컨트롤러가 principal 타입을 {@code instanceof}로 검증하지 않고 무조건 캐스팅할 경우
     * {@code ClassCastException}이 발생하는 상황을 재현하기 위한 헬퍼이다.
     */
    private Authentication genericUserAuthentication(long memberId) {
        User principal = new User(
                "member" + memberId + "@example.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    // -------------------------------------------------------------------------
    // get
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser
    @DisplayName("구독권 조회 요청이 성공하면 200과 구독권 정보를 반환한다")
    void givenExistingSubscription_whenGetSubscription_thenReturns200() throws Exception {
        when(subscriptionService.getSubscriptionInfo(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/subscriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(1L))
                .andExpect(jsonPath("$.memberId").value(1L));

        verify(subscriptionService).getSubscriptionInfo(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("존재하지 않는 구독권 조회 시 400을 반환한다")
    void givenNonExistentSubscription_whenGetSubscription_thenReturns400() throws Exception {
        when(subscriptionService.getSubscriptionInfo(999L))
                .thenThrow(new SubscriptionException("구독권을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/subscriptions/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("구독권을 찾을 수 없습니다."));

        verify(subscriptionService).getSubscriptionInfo(999L);
    }

    // -------------------------------------------------------------------------
    // cancel
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("구독권 취소 요청이 성공하면 200을 반환한다")
    void givenAuthenticatedUser_whenCancelSubscription_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/subscriptions/1/cancel")
                        .with(authentication(memberAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(subscriptionService).cancelSubscription(1L, 1L);
    }

    @Test
    @DisplayName("구독권 취소 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenCancelSubscription_thenReturns400() throws Exception {
        doThrow(new SubscriptionException("본인의 구독권만 취소할 수 있습니다."))
                .when(subscriptionService).cancelSubscription(anyLong(), anyLong());

        mockMvc.perform(post("/api/subscriptions/1/cancel")
                        .with(authentication(memberAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("본인의 구독권만 취소할 수 있습니다."));

        verify(subscriptionService).cancelSubscription(anyLong(), anyLong());
    }

    @Test
    @DisplayName("principal이 CustomUserPrincipal이 아니면 400과 인증 정보 오류 메시지를 반환하고 서비스는 호출되지 않는다")
    void givenNonCustomUserPrincipal_whenCancelSubscription_thenReturns400() throws Exception {
        mockMvc.perform(post("/api/subscriptions/1/cancel")
                        .with(authentication(genericUserAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증 정보가 올바르지 않습니다."));

        verifyNoInteractions(subscriptionService);
    }

    // -------------------------------------------------------------------------
    // 제거된 엔드포인트 회귀 방지 (수동 생성 / 재발급)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("수동 생성 엔드포인트(POST /api/subscriptions)는 더 이상 존재하지 않으므로 404를 반환한다")
    void givenManualCreateEndpointRemoved_whenPostSubscriptions_thenReturns404() throws Exception {
        mockMvc.perform(post("/api/subscriptions")
                        .with(authentication(memberAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verifyNoInteractions(subscriptionService);
    }

    @Test
    @DisplayName("재발급 엔드포인트(POST /api/subscriptions/reissue)는 더 이상 존재하지 않으므로 405를 반환한다"
            + " (경로 자체는 GET /api/subscriptions/{subscriptionId}의 subscriptionId=\"reissue\"로 매치되지만"
            + " 이 경로에 POST를 지원하는 핸들러가 없어 Spring MVC가 404 대신 405를 반환한다)")
    void givenReissueEndpointRemoved_whenPostReissue_thenReturns405() throws Exception {
        mockMvc.perform(post("/api/subscriptions/reissue")
                        .with(authentication(memberAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(subscriptionService);
    }
}
