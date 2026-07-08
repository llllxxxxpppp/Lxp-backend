package com.lcs.lxp.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.lxp.course.dto.request.AddLectureRequest;
import com.lcs.lxp.course.dto.request.AddMissionRequest;
import com.lcs.lxp.course.dto.request.CreateCourseRequest;
import com.lcs.lxp.course.dto.request.UpdateCourseRequest;
import com.lcs.lxp.course.service.CourseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_INSTRUCTOR")
    @DisplayName("강사가 강좌 생성을 요청하면 201 Created를 반환한다")
    void givenInstructorRole_whenCreateCourse_thenReturns201() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("강좌 제목", "강좌 설명", null);

        mockMvc.perform(post("/api/courses")
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
    @WithMockUser(authorities = "ROLE_INSTRUCTOR")
    @DisplayName("강사가 강좌 공개를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenPublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/publish"))
                .andExpect(status().isOk());

        verify(courseService).publishCourse(1L);
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
    @WithMockUser(authorities = "ROLE_INSTRUCTOR")
    @DisplayName("강사가 강좌 비공개를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenUnpublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/unpublish"))
                .andExpect(status().isOk());

        verify(courseService).unpublishCourse(1L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("어드민이 강좌 비공개를 요청하면 200 OK를 반환한다")
    void givenAdminRole_whenUnpublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/unpublish"))
                .andExpect(status().isOk());

        verify(courseService).unpublishCourse(1L);
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
        AddLectureRequest request = new AddLectureRequest("강의 제목", "https://example.com/lecture");

        mockMvc.perform(post("/api/courses/1/lectures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(courseService).addLecture(1L, "강의 제목", "https://example.com/lecture");
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 강의 추가를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenAddLecture_thenReturns403() throws Exception {
        AddLectureRequest request = new AddLectureRequest("강의 제목", "https://example.com/lecture");

        mockMvc.perform(post("/api/courses/1/lectures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    // --- POST /api/courses/{courseId}/lectures/{lectureId}/publish (publishLecture) ---

    @Test
    @WithMockUser(authorities = "ROLE_INSTRUCTOR")
    @DisplayName("강사가 강의 공개를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenPublishLecture_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/lectures/10/publish"))
                .andExpect(status().isOk());

        verify(courseService).publishLecture(1L, 10L);
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
    @WithMockUser(authorities = "ROLE_INSTRUCTOR")
    @DisplayName("강사가 강의 비공개를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenUnpublishLecture_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/lectures/10/unpublish"))
                .andExpect(status().isOk());

        verify(courseService).unpublishLecture(1L, 10L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("어드민이 강의 비공개를 요청하면 200 OK를 반환한다")
    void givenAdminRole_whenUnpublishLecture_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/lectures/10/unpublish"))
                .andExpect(status().isOk());

        verify(courseService).unpublishLecture(1L, 10L);
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
    @WithMockUser(authorities = "ROLE_INSTRUCTOR")
    @DisplayName("강사가 미션 공개를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenPublishMission_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/missions/20/publish"))
                .andExpect(status().isOk());

        verify(courseService).publishMission(1L, 20L);
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
    @WithMockUser(authorities = "ROLE_INSTRUCTOR")
    @DisplayName("강사가 미션 비공개를 요청하면 200 OK를 반환한다")
    void givenInstructorRole_whenUnpublishMission_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/missions/20/unpublish"))
                .andExpect(status().isOk());

        verify(courseService).unpublishMission(1L, 20L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("어드민이 미션 비공개를 요청하면 200 OK를 반환한다")
    void givenAdminRole_whenUnpublishMission_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/missions/20/unpublish"))
                .andExpect(status().isOk());

        verify(courseService).unpublishMission(1L, 20L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("일반 회원이 미션 비공개를 요청하면 403 Forbidden을 반환한다")
    void givenMemberRole_whenUnpublishMission_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/1/missions/20/unpublish"))
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
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("강좌 수정은 역할 제한이 없으므로 일반 회원도 200 OK를 반환한다")
    void givenMemberRole_whenUpdateCourse_thenReturns200() throws Exception {
        UpdateCourseRequest request = new UpdateCourseRequest("수정된 제목", "수정된 설명", null);

        mockMvc.perform(patch("/api/courses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(courseService).updateCourse(1L, "수정된 제목", "수정된 설명", null);
    }
}
