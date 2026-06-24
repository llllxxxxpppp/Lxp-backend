package com.lcs.lxp.subscription.presentation;

import com.lcs.lxp.security.jwt.JwtTokenProvider;
import com.lcs.lxp.security.refresh.RefreshService;
import com.lcs.lxp.subscription.application.dto.response.SubscriptionResponse;
import com.lcs.lxp.subscription.application.service.SubscriptionService;
import com.lcs.lxp.subscription.domain.exception.SubscriptionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        return new SubscriptionResponse(1L, 1L, "ACTIVE", OffsetDateTime.now(), OffsetDateTime.now().plusDays(31), OffsetDateTime.now());
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("구독권 생성 요청이 성공하면 201과 구독권 정보를 반환한다")
    void givenAuthenticatedUser_whenCreateSubscription_thenReturns201() throws Exception {
        when(subscriptionService.createSubscription(1L)).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/subscriptions").with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriptionId").value(1L))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(subscriptionService).createSubscription(1L);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("구독권 생성 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenCreateSubscription_thenReturns400() throws Exception {
        doThrow(new SubscriptionException("이미 활성 구독권이 있습니다."))
                .when(subscriptionService).createSubscription(anyLong());

        mockMvc.perform(post("/api/subscriptions").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 활성 구독권이 있습니다."));

        verify(subscriptionService).createSubscription(anyLong());
    }

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

    @Test
    @WithMockUser(username = "1")
    @DisplayName("구독권 취소 요청이 성공하면 200을 반환한다")
    void givenAuthenticatedUser_whenCancelSubscription_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/subscriptions/1/cancel").with(csrf()))
                .andExpect(status().isOk());

        verify(subscriptionService).cancelSubscription(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("구독권 취소 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenCancelSubscription_thenReturns400() throws Exception {
        doThrow(new SubscriptionException("본인의 구독권만 취소할 수 있습니다."))
                .when(subscriptionService).cancelSubscription(anyLong(), anyLong());

        mockMvc.perform(post("/api/subscriptions/1/cancel").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("본인의 구독권만 취소할 수 있습니다."));

        verify(subscriptionService).cancelSubscription(anyLong(), anyLong());
    }

    @Test
    @WithMockUser
    @DisplayName("재발급 요청이 성공하면 200을 반환한다")
    void givenValidRequest_whenReissue_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/subscriptions/reissue").with(csrf()))
                .andExpect(status().isOk());

        verify(subscriptionService).reissueExpiring();
    }

    @Test
    @WithMockUser
    @DisplayName("재발급 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenReissue_thenReturns400() throws Exception {
        doThrow(new SubscriptionException("재발급 처리 중 오류가 발생했습니다."))
                .when(subscriptionService).reissueExpiring();

        mockMvc.perform(post("/api/subscriptions/reissue").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("재발급 처리 중 오류가 발생했습니다."));

        verify(subscriptionService).reissueExpiring();
    }
}
