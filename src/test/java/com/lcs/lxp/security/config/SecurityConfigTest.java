package com.lcs.lxp.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.lxp.course.dto.request.AddLectureRequest;
import com.lcs.lxp.course.dto.request.AddMissionRequest;
import com.lcs.lxp.course.dto.request.CreateCourseRequest;
import com.lcs.lxp.course.dto.request.ReorderRequest;
import com.lcs.lxp.course.dto.request.UpdateCourseRequest;
import com.lcs.lxp.security.principal.CustomUserPrincipal;
import java.util.List;
import com.lcs.lxp.course.service.CourseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseService courseService;

    // --- POST /api/courses (createCourse) ---

    /**
     * {@code createCourse}는 컨트롤러 내부에서 {@code authentication.getPrincipal()}을
     * {@link CustomUserPrincipal}로 캐스팅하여 강사 ID를 꺼낸다. {@code @WithMockUser}가 생성하는
     * principal은 일반 스프링 시큐리티 {@code User}(UserDetails)로 {@code CustomUserPrincipal}이
     * 아니므로 그대로 사용하면 {@code ClassCastException}이 발생한다. 실제 인증 흐름과 동일하게
     * {@code CustomUserPrincipal}을 담은 인증 객체를 직접 주입한다.
     */
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
     * COURSE-09: 소유권 검증 대상 엔드포인트(수정/공개/비공개/삭제)는 컨트롤러 내부에서
     * {@code CustomUserPrincipal}을 캐스팅하여 요청자 ID/어드민 여부를 추출하므로,
     * 어드민·일반 회원 시나리오도 동일하게 {@code CustomUserPrincipal}을 담은 인증 객체로 대체한다.
     */
    private Authentication adminAuthentication(long adminId) {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                adminId,
                "admin" + adminId + "@test.com",
                "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    private Authentication memberAuthentication(long memberId) {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                memberId,
                "member" + memberId + "@test.com",
                "",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")),
                false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("강사가 강좌 생성을 요청하면 201 Created를 반환한다")
    void givenInstructorRole_whenCreateCourse_thenReturns201() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("강좌 제목", "강좌 설명", null);

        mockMvc.perform(post("/api/courses")
                        .with(authentication(instructorAuthentication(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(courseService).createCourse(1L, "강좌 제목", "강좌 설명", null);
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 강좌 생성을 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenCreateCourse_thenReturns403() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("강좌 제목", "강좌 설명", null);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_ADMIN")
    @DisplayName("어드민이 강좌 생성을 요청하면 403 Forbidden을 반환한다")
    void givenAdminRole_whenCreateCourse_thenReturns403() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("강좌 제목", "강좌 설명", null);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("미인증 사용자가 강좌 생성을 요청하면 401 Unauthorized을 반환한다")
    void givenUnauthenticated_whenCreateCourse_thenReturns401() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("강좌 제목", "강좌 설명", null);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    // --- POST /api/courses/{courseId}/publish (publishCourse) ---

    @Test
    @DisplayName("강사가 강좌 공개를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenPublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/publish")
                        .with(authentication(instructorAuthentication(1L))))
                .andExpect(status().isOk());

        verify(courseService).publishCourse(1L, 1L, false);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 강좌 공개를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenPublishCourse_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/1/publish"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    // --- POST /api/courses/{courseId}/unpublish (unpublishCourse) ---

    @Test
    @DisplayName("강사가 강좌 비공개를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenUnpublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/unpublish")
                        .with(authentication(instructorAuthentication(1L))))
                .andExpect(status().isOk());

        verify(courseService).unpublishCourse(1L, 1L, false);
    }

    @Test
    @DisplayName("어드민이 강좌 비공개를 요청하면 200 OK를 반환한다")
    void givenAdminRole_whenUnpublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/unpublish")
                        .with(authentication(adminAuthentication(99L))))
                .andExpect(status().isOk());

        verify(courseService).unpublishCourse(1L, 99L, true);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 강좌 비공개를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenUnpublishCourse_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/1/unpublish"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    // --- POST /api/courses/{courseId}/lectures (addLecture) ---

    @Test
    @WithMockUser(authorities = "ROLE_INSTRUCTOR")
    @DisplayName("강사가 강의 추가를 요청하면 201 Created를 반환한다")
    void givenInstructorRole_whenAddLecture_thenReturns201() throws Exception {
        AddLectureRequest request = new AddLectureRequest("강의 제목", "https://example.com/lecture", "mp4");

        mockMvc.perform(post("/api/courses/1/lectures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(courseService).addLecture(1L, "강의 제목", "https://example.com/lecture", "mp4");
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 강의 추가를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenAddLecture_thenReturns403() throws Exception {
        AddLectureRequest request = new AddLectureRequest("강의 제목", "https://example.com/lecture", "mp4");

        mockMvc.perform(post("/api/courses/1/lectures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    // --- POST /api/courses/{courseId}/lectures/{lectureId}/publish (publishLecture) ---

    @Test
    @DisplayName("강사가 강의 공개를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenPublishLecture_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/lectures/10/publish")
                        .with(authentication(instructorAuthentication(1L))))
                .andExpect(status().isOk());

        verify(courseService).publishLecture(1L, 10L, 1L, false);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 강의 공개를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenPublishLecture_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/1/lectures/10/publish"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    // --- POST /api/courses/{courseId}/lectures/{lectureId}/unpublish (unpublishLecture) ---

    @Test
    @DisplayName("강사가 강의 비공개를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenUnpublishLecture_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/lectures/10/unpublish")
                        .with(authentication(instructorAuthentication(1L))))
                .andExpect(status().isOk());

        verify(courseService).unpublishLecture(1L, 10L, 1L, false);
    }

    @Test
    @DisplayName("어드민이 강의 비공개를 요청하면 200 OK를 반환한다")
    void givenAdminRole_whenUnpublishLecture_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/lectures/10/unpublish")
                        .with(authentication(adminAuthentication(99L))))
                .andExpect(status().isOk());

        verify(courseService).unpublishLecture(1L, 10L, 99L, true);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 강의 비공개를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenUnpublishLecture_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/1/lectures/10/unpublish"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    // --- POST /api/courses/{courseId}/missions (addMission) ---

    @Test
    @WithMockUser(authorities = "ROLE_INSTRUCTOR")
    @DisplayName("강사가 미션 추가를 요청하면 201 Created를 반환한다")
    void givenInstructorRole_whenAddMission_thenReturns201() throws Exception {
        AddMissionRequest request = new AddMissionRequest("미션 제목", "미션 내용");

        mockMvc.perform(post("/api/courses/1/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(courseService).addMission(1L, "미션 제목", "미션 내용");
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 미션 추가를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenAddMission_thenReturns403() throws Exception {
        AddMissionRequest request = new AddMissionRequest("미션 제목", "미션 내용");

        mockMvc.perform(post("/api/courses/1/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    // --- POST /api/courses/{courseId}/missions/{missionId}/publish (publishMission) ---

    @Test
    @DisplayName("강사가 미션 공개를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenPublishMission_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/missions/20/publish")
                        .with(authentication(instructorAuthentication(1L))))
                .andExpect(status().isOk());

        verify(courseService).publishMission(1L, 20L, 1L, false);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 미션 공개를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenPublishMission_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/1/missions/20/publish"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    // --- POST /api/courses/{courseId}/missions/{missionId}/unpublish (unpublishMission) ---

    @Test
    @DisplayName("강사가 미션 비공개를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenUnpublishMission_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/missions/20/unpublish")
                        .with(authentication(instructorAuthentication(1L))))
                .andExpect(status().isOk());

        verify(courseService).unpublishMission(1L, 20L, 1L, false);
    }

    @Test
    @DisplayName("어드민이 미션 비공개를 요청하면 200 OK를 반환한다")
    void givenAdminRole_whenUnpublishMission_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/missions/20/unpublish")
                        .with(authentication(adminAuthentication(99L))))
                .andExpect(status().isOk());

        verify(courseService).unpublishMission(1L, 20L, 99L, true);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 미션 비공개를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenUnpublishMission_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/1/missions/20/unpublish"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    // --- DELETE /api/courses/{courseId} (deleteCourse) ---

    @Test
    @DisplayName("강사가 강좌 삭제를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenDeleteCourse_thenReturns200() throws Exception {
        mockMvc.perform(delete("/api/courses/1")
                        .with(authentication(instructorAuthentication(1L))))
                .andExpect(status().isOk());

        verify(courseService).deleteCourse(1L, 1L, false);
    }

    @Test
    @DisplayName("어드민이 강좌 삭제를 요청하면 200 OK를 반환한다")
    void givenAdminRole_whenDeleteCourse_thenReturns200() throws Exception {
        mockMvc.perform(delete("/api/courses/1")
                        .with(authentication(adminAuthentication(99L))))
                .andExpect(status().isOk());

        verify(courseService).deleteCourse(1L, 99L, true);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 강좌 삭제를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenDeleteCourse_thenReturns403() throws Exception {
        mockMvc.perform(delete("/api/courses/1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("미인증 사용자가 강좌 삭제를 요청하면 401 Unauthorized을 반환한다")
    void givenUnauthenticated_whenDeleteCourse_thenReturns401() throws Exception {
        mockMvc.perform(delete("/api/courses/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    // --- DELETE /api/courses/{courseId}/lectures/{lectureId} (deleteLecture) ---

    @Test
    @DisplayName("강사가 강의 삭제를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenDeleteLecture_thenReturns200() throws Exception {
        mockMvc.perform(delete("/api/courses/1/lectures/10")
                        .with(authentication(instructorAuthentication(1L))))
                .andExpect(status().isOk());

        verify(courseService).deleteLecture(1L, 10L, 1L, false);
    }

    @Test
    @DisplayName("어드민이 강의 삭제를 요청하면 200 OK를 반환한다")
    void givenAdminRole_whenDeleteLecture_thenReturns200() throws Exception {
        mockMvc.perform(delete("/api/courses/1/lectures/10")
                        .with(authentication(adminAuthentication(99L))))
                .andExpect(status().isOk());

        verify(courseService).deleteLecture(1L, 10L, 99L, true);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 강의 삭제를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenDeleteLecture_thenReturns403() throws Exception {
        mockMvc.perform(delete("/api/courses/1/lectures/10"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    // --- DELETE /api/courses/{courseId}/missions/{missionId} (deleteMission) ---

    @Test
    @DisplayName("강사가 미션 삭제를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenDeleteMission_thenReturns200() throws Exception {
        mockMvc.perform(delete("/api/courses/1/missions/20")
                        .with(authentication(instructorAuthentication(1L))))
                .andExpect(status().isOk());

        verify(courseService).deleteMission(1L, 20L, 1L, false);
    }

    @Test
    @DisplayName("어드민이 미션 삭제를 요청하면 200 OK를 반환한다")
    void givenAdminRole_whenDeleteMission_thenReturns200() throws Exception {
        mockMvc.perform(delete("/api/courses/1/missions/20")
                        .with(authentication(adminAuthentication(99L))))
                .andExpect(status().isOk());

        verify(courseService).deleteMission(1L, 20L, 99L, true);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 미션 삭제를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenDeleteMission_thenReturns403() throws Exception {
        mockMvc.perform(delete("/api/courses/1/missions/20"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    // --- Regression tests: endpoints with no role restriction ---

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("강좌 목록 조회는 역할 제한이 없으므로 일반 회원도 200 OK를 반환한다")
    void givenMemberRole_whenGetCourses_thenReturns200() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk());

        verify(courseService).getCourses(null, 0, 10);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("강좌 조회는 역할 제한이 없으므로 일반 회원도 200 OK를 반환한다")
    void givenMemberRole_whenGetCourseSummary_thenReturns200() throws Exception {
        mockMvc.perform(get("/api/courses/1"))
                .andExpect(status().isOk());

        verify(courseService).getCourseSummary(1L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("강좌 상세 조회는 역할 제한이 없으므로 일반 회원도 200 OK를 반환한다")
    void givenMemberRole_whenGetCourseDetail_thenReturns200() throws Exception {
        mockMvc.perform(get("/api/courses/1/detail"))
                .andExpect(status().isOk());

        verify(courseService).getCourseDetail(1L);
    }

    @Test
    @DisplayName("강좌 수정은 역할 제한이 없으므로 일반 회원도 200 OK를 반환한다")
    void givenMemberRole_whenUpdateCourse_thenReturns200() throws Exception {
        UpdateCourseRequest request = new UpdateCourseRequest("수정된 제목", "수정된 설명", null);

        mockMvc.perform(patch("/api/courses/1")
                        .with(authentication(memberAuthentication(5L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(courseService).updateCourse(1L, "수정된 제목", "수정된 설명", null, 5L, false);
    }

    // --- PATCH /api/courses/{courseId}/reorder (reorderItems) ---

    @Test
    @WithMockUser(authorities = "ROLE_INSTRUCTOR")
    @DisplayName("강사가 순서 변경을 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenReorderItems_thenReturns200() throws Exception {
        ReorderRequest request = new ReorderRequest(List.of(
                new ReorderRequest.Item(ReorderRequest.Type.LECTURE, 10L),
                new ReorderRequest.Item(ReorderRequest.Type.MISSION, 20L)));

        mockMvc.perform(patch("/api/courses/1/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(courseService).reorderItems(1L, List.of("LECTURE", "MISSION"), List.of(10L, 20L));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("어드민이 순서 변경을 요청하면 200 OK를 반환한다")
    void givenAdminRole_whenReorderItems_thenReturns200() throws Exception {
        ReorderRequest request = new ReorderRequest(List.of(
                new ReorderRequest.Item(ReorderRequest.Type.LECTURE, 10L),
                new ReorderRequest.Item(ReorderRequest.Type.MISSION, 20L)));

        mockMvc.perform(patch("/api/courses/1/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(courseService).reorderItems(1L, List.of("LECTURE", "MISSION"), List.of(10L, 20L));
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 순서 변경을 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenReorderItems_thenReturns403() throws Exception {
        ReorderRequest request = new ReorderRequest(List.of(
                new ReorderRequest.Item(ReorderRequest.Type.LECTURE, 10L),
                new ReorderRequest.Item(ReorderRequest.Type.MISSION, 20L)));

        mockMvc.perform(patch("/api/courses/1/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("미인증 사용자가 순서 변경을 요청하면 401 Unauthorized를 반환한다")
    void givenUnauthenticated_whenReorderItems_thenReturns401() throws Exception {
        ReorderRequest request = new ReorderRequest(List.of(
                new ReorderRequest.Item(ReorderRequest.Type.LECTURE, 10L),
                new ReorderRequest.Item(ReorderRequest.Type.MISSION, 20L)));

        mockMvc.perform(patch("/api/courses/1/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    // --- SEC-05: Swagger/OpenAPI 명세서 열람 (미인증 접근 허용) ---

    @Test
    @DisplayName("미인증 사용자가 OpenAPI 문서를 요청하면 200 OK를 반환한다")
    void givenUnauthenticated_whenAccessApiDocs_thenReturns200() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("미인증 사용자가 Swagger UI를 요청하면 200 OK를 반환한다")
    void givenUnauthenticated_whenAccessSwaggerUi_thenReturns200() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        verifyNoInteractions(courseService);
    }
}
