package com.lcs.lxp.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.lxp.member.dto.request.ChangePasswordRequest;
import com.lcs.lxp.member.dto.request.UpdateInstructorProfileRequest;
import com.lcs.lxp.member.dto.response.UserResponseDTO;
import com.lcs.lxp.member.model.MemberRole;
import com.lcs.lxp.member.service.MemberService;
import com.lcs.lxp.security.principal.CustomUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class MemberSelfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    /**
     * {@code resolveMemberId}는 컨트롤러 내부에서 {@code authentication.getPrincipal()}을
     * {@link CustomUserPrincipal}로 캐스팅하여 회원 ID를 꺼낸다. {@code @WithMockUser}가 생성하는
     * principal은 일반 스프링 시큐리티 {@code User}(UserDetails)로 {@code CustomUserPrincipal}이
     * 아니므로 그대로 사용하면 {@code ClassCastException}이 발생한다. 실제 인증 흐름과 동일하게
     * {@code CustomUserPrincipal}을 담은 인증 객체를 직접 주입한다.
     */
    private Authentication memberAuthentication(long memberId) {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                memberId,
                "member" + memberId + "@test.com",
                "",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")),
                false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    private Authentication instructorAuthentication(long instructorId) {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                instructorId,
                "instructor" + instructorId + "@test.com",
                "",
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")),
                false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    /**
     * {@code CustomUserPrincipal}이 아닌 일반 스프링 시큐리티 {@code UserDetails}({@code User}) 기반
     * 인증 객체. 컨트롤러가 principal 타입을 {@code instanceof}로 검증하지 않고 무조건 캐스팅할 경우
     * {@code ClassCastException}(500)이 발생하는 상황을 재현하기 위한 헬퍼이다.
     */
    private Authentication genericUserAuthentication(long memberId) {
        User principal = new User(
                "member" + memberId + "@test.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    // --- PATCH /api/members/me/password (changePassword) ---

    @Test
    @DisplayName("인증된 회원이 비밀번호 변경을 요청하면 204 No Content를 반환한다")
    void givenAuthenticatedUser_whenChangePassword_thenReturns204() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "new_password");

        mockMvc.perform(patch("/api/members/me/password")
                        .with(authentication(memberAuthentication(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(memberService).changePassword(1L, "current_password", "new_password");
    }

    @Test
    @DisplayName("미인증 사용자가 비밀번호 변경을 요청하면 401 Unauthorized를 반환한다")
    void givenUnauthenticatedUser_whenChangePassword_thenReturns401() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "new_password");

        mockMvc.perform(patch("/api/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(memberService);
    }

    @Test
    @DisplayName("principal이 CustomUserPrincipal이 아니면 400과 인증 정보 오류 메시지를 반환하고 서비스는 호출되지 않는다")
    void givenNonCustomUserPrincipal_whenChangePassword_thenReturns400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "new_password");

        mockMvc.perform(patch("/api/members/me/password")
                        .with(authentication(genericUserAuthentication(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증 정보가 올바르지 않습니다."));

        verifyNoInteractions(memberService);
    }

    // --- PATCH /api/members/me/instructor-profile (updateInstructorProfile) ---

    @Test
    @DisplayName("인증된 강사가 프로필 변경을 요청하면 200 OK를 반환하고 UserResponseDTO를 반환한다")
    void givenAuthenticatedInstructor_whenUpdateInstructorProfile_thenReturns200() throws Exception {
        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest(
                "홍길동", "https://example.com/profile.jpg", "안녕하세요.");

        UserResponseDTO responseDTO = new UserResponseDTO(1L, "instructor@example.com", MemberRole.INSTRUCTOR);
        when(memberService.updateInstructorProfile(
                eq(1L),
                eq("홍길동"),
                eq("https://example.com/profile.jpg"),
                eq("안녕하세요.")
        )).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/members/me/instructor-profile")
                        .with(authentication(instructorAuthentication(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(memberService).updateInstructorProfile(
                1L,
                "홍길동",
                "https://example.com/profile.jpg",
                "안녕하세요.");
    }

    @Test
    @DisplayName("미인증 사용자가 프로필 변경을 요청하면 401 Unauthorized를 반환한다")
    void givenUnauthenticatedUser_whenUpdateInstructorProfile_thenReturns401() throws Exception {
        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest(
                "홍길동", "https://example.com/profile.jpg", "안녕하세요.");

        mockMvc.perform(patch("/api/members/me/instructor-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(memberService);
    }

    // --- DELETE /api/members/me (withdrawMember) ---

    @Test
    @DisplayName("인증된 회원이 탈퇴를 요청하면 204 No Content를 반환한다")
    void givenAuthenticatedUser_whenWithdraw_thenReturns204() throws Exception {
        mockMvc.perform(delete("/api/members/me")
                        .with(authentication(memberAuthentication(1L))))
                .andExpect(status().isNoContent());

        verify(memberService).withdrawMember(1L);
    }

    @Test
    @DisplayName("미인증 사용자가 탈퇴를 요청하면 401 Unauthorized를 반환한다")
    void givenUnauthenticatedUser_whenWithdraw_thenReturns401() throws Exception {
        mockMvc.perform(delete("/api/members/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(memberService);
    }
}
