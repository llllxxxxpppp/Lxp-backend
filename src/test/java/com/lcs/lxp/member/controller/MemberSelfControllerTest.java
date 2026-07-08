package com.lcs.lxp.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.lxp.member.dto.request.ChangePasswordRequest;
import com.lcs.lxp.member.dto.request.UpdateInstructorProfileRequest;
import com.lcs.lxp.member.dto.response.UserResponseDTO;
import com.lcs.lxp.member.model.MemberRole;
import com.lcs.lxp.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    // --- PATCH /api/members/me/password (changePassword) ---

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_MEMBER")
    @DisplayName("인증된 회원이 비밀번호 변경을 요청하면 204 No Content를 반환한다")
    void givenAuthenticatedUser_whenChangePassword_thenReturns204() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "new_password");

        mockMvc.perform(patch("/api/members/me/password")
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

    // --- PATCH /api/members/me/instructor-profile (updateInstructorProfile) ---

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_INSTRUCTOR")
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
    @WithMockUser(username = "1", authorities = "ROLE_MEMBER")
    @DisplayName("인증된 회원이 탈퇴를 요청하면 204 No Content를 반환한다")
    void givenAuthenticatedUser_whenWithdraw_thenReturns204() throws Exception {
        mockMvc.perform(delete("/api/members/me"))
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
