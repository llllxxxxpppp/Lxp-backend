package com.lcs.lxp.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.lxp.member.dto.request.RegisterInstructorRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AdminMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    // --- POST /api/admin/members/instructors (registerInstructor) ---

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("어드민이 강사 등록을 요청하면 201 Created를 반환한다")
    void givenAdminRole_whenRegisterInstructor_thenReturns201() throws Exception {
        RegisterInstructorRequest request = new RegisterInstructorRequest(
                "instructor@example.com",
                "password123",
                "홍길동",
                "https://example.com/image.jpg",
                "안녕하세요"
        );

        UserResponseDTO mockResponse = new UserResponseDTO(1L, "instructor@example.com", MemberRole.INSTRUCTOR);
        when(memberService.registerInstructor(
                "instructor@example.com",
                "password123",
                "홍길동",
                "https://example.com/image.jpg",
                "안녕하세요"
        )).thenReturn(mockResponse);

        mockMvc.perform(post("/api/admin/members/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(memberService).registerInstructor(
                "instructor@example.com",
                "password123",
                "홍길동",
                "https://example.com/image.jpg",
                "안녕하세요"
        );
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 강사 등록을 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenRegisterInstructor_thenReturns403() throws Exception {
        RegisterInstructorRequest request = new RegisterInstructorRequest(
                "instructor@example.com",
                "password123",
                "홍길동",
                "https://example.com/image.jpg",
                "안녕하세요"
        );

        mockMvc.perform(post("/api/admin/members/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(memberService);
    }

    @Test
    @WithMockUser(authorities = "ROLE_INSTRUCTOR")
    @DisplayName("강사가 강사 등록을 요청하면 403 Forbidden을 반환한다")
    void givenInstructorRole_whenRegisterInstructor_thenReturns403() throws Exception {
        RegisterInstructorRequest request = new RegisterInstructorRequest(
                "instructor@example.com",
                "password123",
                "홍길동",
                "https://example.com/image.jpg",
                "안녕하세요"
        );

        mockMvc.perform(post("/api/admin/members/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(memberService);
    }

    @Test
    @DisplayName("미인증 사용자가 강사 등록을 요청하면 401 Unauthorized을 반환한다")
    void givenUnauthenticated_whenRegisterInstructor_thenReturns401() throws Exception {
        RegisterInstructorRequest request = new RegisterInstructorRequest(
                "instructor@example.com",
                "password123",
                "홍길동",
                "https://example.com/image.jpg",
                "안녕하세요"
        );

        mockMvc.perform(post("/api/admin/members/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(memberService);
    }

    // --- POST /api/admin/members/instructors/{instructorId}/suspend (suspendInstructor) ---

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("어드민이 강사 정지를 요청하면 200 OK를 반환한다")
    void givenAdminRole_whenSuspendInstructor_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/admin/members/instructors/1/suspend"))
                .andExpect(status().isOk());

        verify(memberService).suspendInstructor(1L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 강사 정지를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenSuspendInstructor_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/admin/members/instructors/1/suspend"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(memberService);
    }

    @Test
    @DisplayName("미인증 사용자가 강사 정지를 요청하면 401 Unauthorized을 반환한다")
    void givenUnauthenticated_whenSuspendInstructor_thenReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/members/instructors/1/suspend"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(memberService);
    }
}
