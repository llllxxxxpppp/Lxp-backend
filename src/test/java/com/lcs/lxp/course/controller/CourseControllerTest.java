package com.lcs.lxp.course.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.lxp.course.dto.request.AddLectureRequest;
import com.lcs.lxp.course.dto.request.AddMissionRequest;
import com.lcs.lxp.course.dto.request.CreateCourseRequest;
import com.lcs.lxp.course.dto.request.ReorderRequest;
import com.lcs.lxp.course.dto.request.UpdateCourseRequest;
import com.lcs.lxp.course.dto.response.CourseDetailResponse;
import com.lcs.lxp.course.dto.response.CourseItemResponse;
import com.lcs.lxp.course.dto.response.CoursePageResponse;
import com.lcs.lxp.course.dto.response.CourseSummaryResponse;
import com.lcs.lxp.course.dto.response.LectureResponse;
import com.lcs.lxp.course.dto.response.MissionResponse;
import com.lcs.lxp.course.exception.CourseAccessDeniedException;
import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.service.CourseService;
import com.lcs.lxp.security.jwt.JwtTokenProvider;
import com.lcs.lxp.security.principal.CustomUserPrincipal;
import com.lcs.lxp.security.refresh.RefreshService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RefreshService refreshService;

    @MockitoBean
    private CourseService courseService;

    @Autowired
    private ObjectMapper objectMapper;

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
     * COURSE-09: 소유권 검증 대상 엔드포인트(수정/공개/비공개/삭제)는 어드민 요청도 처리해야 하므로
     * {@code ROLE_ADMIN} 권한을 가진 {@link CustomUserPrincipal}을 담은 인증 객체를 제공한다.
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

    /**
     * COURSE-11: 컨트롤러가 {@code authentication.getPrincipal()}을 {@link CustomUserPrincipal}로
     * 무조건 캐스팅하는 경우를 방어하는지 검증하기 위해, 일반 스프링 시큐리티 {@link User}(UserDetails)
     * 기반의 principal을 담은 인증 객체를 제공한다.
     */
    private Authentication genericUserAuthentication(long userId) {
        User principal = new User(
                "user" + userId + "@test.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")));
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    // --- getCourses ---

    @Test
    @WithMockUser
    @DisplayName("키워드 없이 강좌 목록 조회 시 200과 페이지 정보를 반환한다")
    void givenNoKeyword_whenGetCourses_thenReturns200WithPage() throws Exception {
        CourseSummaryResponse summary = new CourseSummaryResponse(1L, 1L, "강좌 제목", "PUBLIC", null);
        CoursePageResponse pageResponse = new CoursePageResponse(List.of(summary), 0, 10, 1L, 1, true);
        when(courseService.getCourses(null, 0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses[0].courseId").value(1L))
                .andExpect(jsonPath("$.courses[0].title").value("강좌 제목"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(courseService).getCourses(null, 0, 10);
    }

    @Test
    @WithMockUser
    @DisplayName("키워드로 강좌 목록 검색 시 200과 검색된 페이지 정보를 반환한다")
    void givenKeyword_whenGetCourses_thenReturns200WithFilteredPage() throws Exception {
        CourseSummaryResponse summary = new CourseSummaryResponse(1L, 1L, "Java 강좌", "PUBLIC", null);
        CoursePageResponse pageResponse = new CoursePageResponse(List.of(summary), 0, 10, 1L, 1, true);
        when(courseService.getCourses("Java", 0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/courses").param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses[0].title").value("Java 강좌"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(courseService).getCourses("Java", 0, 10);
    }

    @Test
    @WithMockUser
    @DisplayName("페이지 파라미터로 강좌 목록 조회 시 해당 페이지 결과를 반환한다")
    void givenPageParams_whenGetCourses_thenReturns200WithPaginatedResult() throws Exception {
        CoursePageResponse pageResponse = new CoursePageResponse(List.of(), 1, 5, 0L, 0, true);
        when(courseService.getCourses(null, 1, 5)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/courses").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));

        verify(courseService).getCourses(null, 1, 5);
    }

    // --- getCourseSummary ---

    @Test
    @WithMockUser
    @DisplayName("강좌 요약 조회 요청이 성공하면 200과 요약 정보를 반환한다")
    void givenExistingCourse_whenGetCourseSummary_thenReturns200WithSummary() throws Exception {
        CourseSummaryResponse summary = new CourseSummaryResponse(1L, 1L, "강좌 제목", "PRIVATE", null);
        when(courseService.getCourseSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(1L))
                .andExpect(jsonPath("$.instructorId").value(1L))
                .andExpect(jsonPath("$.title").value("강좌 제목"))
                .andExpect(jsonPath("$.status").value("PRIVATE"));

        verify(courseService).getCourseSummary(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("존재하지 않는 강좌 요약 조회 시 400을 반환한다")
    void givenNonExistentCourse_whenGetCourseSummary_thenReturns400() throws Exception {
        when(courseService.getCourseSummary(999L))
                .thenThrow(new CourseException("강좌를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/courses/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("강좌를 찾을 수 없습니다."));

        verify(courseService).getCourseSummary(999L);
    }

    // --- getCourseDetail ---

    @Test
    @WithMockUser
    @DisplayName("강좌 상세 조회 요청이 성공하면 200과 강의·미션을 포함한 상세 정보를 반환한다")
    void givenExistingCourse_whenGetCourseDetail_thenReturns200WithDetail() throws Exception {
        List<LectureResponse> lectures = List.of(new LectureResponse(10L, "강의 제목", "PRIVATE", "mp4", 1));
        List<MissionResponse> missions = List.of(new MissionResponse(20L, "미션 제목", "PRIVATE", 2));
        List<CourseItemResponse> items = List.of(
                new CourseItemResponse("LECTURE", 10L, "강의 제목", "PRIVATE", 1),
                new CourseItemResponse("MISSION", 20L, "미션 제목", "PRIVATE", 2));
        CourseDetailResponse detail =
                new CourseDetailResponse(1L, 1L, "강좌 제목", "PRIVATE", "강좌 설명", null, lectures, missions, items);
        when(courseService.getCourseDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/courses/1/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(1L))
                .andExpect(jsonPath("$.lectures[0].lectureId").value(10L))
                .andExpect(jsonPath("$.lectures[0].title").value("강의 제목"))
                .andExpect(jsonPath("$.lectures[0].contentType").value("mp4"))
                .andExpect(jsonPath("$.lectures[0].sortOrder").value(1))
                .andExpect(jsonPath("$.missions[0].missionId").value(20L))
                .andExpect(jsonPath("$.missions[0].title").value("미션 제목"))
                .andExpect(jsonPath("$.missions[0].sortOrder").value(2))
                .andExpect(jsonPath("$.items[0].type").value("LECTURE"))
                .andExpect(jsonPath("$.items[0].id").value(10L))
                .andExpect(jsonPath("$.items[0].title").value("강의 제목"))
                .andExpect(jsonPath("$.items[0].sortOrder").value(1))
                .andExpect(jsonPath("$.items[1].type").value("MISSION"))
                .andExpect(jsonPath("$.items[1].id").value(20L))
                .andExpect(jsonPath("$.items[1].title").value("미션 제목"))
                .andExpect(jsonPath("$.items[1].sortOrder").value(2));

        verify(courseService).getCourseDetail(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("존재하지 않는 강좌 상세 조회 시 400을 반환한다")
    void givenNonExistentCourse_whenGetCourseDetail_thenReturns400() throws Exception {
        when(courseService.getCourseDetail(999L))
                .thenThrow(new CourseException("강좌를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/courses/999/detail"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("강좌를 찾을 수 없습니다."));

        verify(courseService).getCourseDetail(999L);
    }

    @Test
    @DisplayName("강좌 생성 요청이 성공하면 201을 반환한다")
    void givenValidRequest_whenCreateCourse_thenReturns201() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("강좌 제목", "강좌 설명", null);

        mockMvc.perform(post("/api/courses")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(courseService).createCourse(1L, "강좌 제목", "강좌 설명", null);
    }

    @Test
    @DisplayName("강좌 생성 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenCreateCourse_thenReturns400() throws Exception {
        doThrow(new CourseException("접근 권한이 없습니다."))
                .when(courseService).createCourse(anyLong(), anyString(), anyString(), nullable(String.class));

        CreateCourseRequest request = new CreateCourseRequest("강좌 제목", "강좌 설명", null);

        mockMvc.perform(post("/api/courses")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));

        verify(courseService).createCourse(anyLong(), anyString(), anyString(), nullable(String.class));
    }

    @Test
    @DisplayName("CustomUserPrincipal이 아닌 인증 객체로 강좌 생성 요청을 하면 400을 반환한다")
    void givenNonCustomUserPrincipalAuthentication_whenCreateCourse_thenReturns400() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("강좌 제목", "강좌 설명", null);

        mockMvc.perform(post("/api/courses")
                        .with(authentication(genericUserAuthentication(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증 정보가 올바르지 않습니다."));

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("강좌를 작성한 강사 본인이 수정 요청을 하면 200을 반환한다")
    void givenOwnerInstructor_whenUpdateCourse_thenReturns200() throws Exception {
        UpdateCourseRequest request = new UpdateCourseRequest("수정된 제목", "수정된 설명", null);

        mockMvc.perform(patch("/api/courses/1")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(courseService).updateCourse(1L, "수정된 제목", "수정된 설명", null, 1L, false);
    }

    @Test
    @DisplayName("어드민이 강좌 수정 요청을 하면 소유자가 아니어도 200을 반환한다")
    void givenAdmin_whenUpdateCourse_thenReturns200() throws Exception {
        UpdateCourseRequest request = new UpdateCourseRequest("수정된 제목", "수정된 설명", null);

        mockMvc.perform(patch("/api/courses/1")
                        .with(authentication(adminAuthentication(99L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(courseService).updateCourse(1L, "수정된 제목", "수정된 설명", null, 99L, true);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 수정 요청을 하면 403을 반환한다")
    void givenOtherInstructor_whenUpdateCourse_thenReturns403() throws Exception {
        doThrow(new CourseAccessDeniedException("작성한 강사만 접근할 수 있습니다."))
                .when(courseService).updateCourse(1L, "수정된 제목", "수정된 설명", null, 2L, false);
        UpdateCourseRequest request = new UpdateCourseRequest("수정된 제목", "수정된 설명", null);

        mockMvc.perform(patch("/api/courses/1")
                        .with(authentication(instructorAuthentication(2L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("작성한 강사만 접근할 수 있습니다."));

        verify(courseService).updateCourse(1L, "수정된 제목", "수정된 설명", null, 2L, false);
    }

    @Test
    @DisplayName("강좌 수정 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenUpdateCourse_thenReturns400() throws Exception {
        doThrow(new CourseException("공개 상태에서는 강좌를 수정할 수 없습니다."))
                .when(courseService).updateCourse(
                        anyLong(), anyString(), anyString(), nullable(String.class), anyLong(), anyBoolean());

        UpdateCourseRequest request = new UpdateCourseRequest("수정된 제목", "수정된 설명", null);

        mockMvc.perform(patch("/api/courses/1")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("공개 상태에서는 강좌를 수정할 수 없습니다."));

        verify(courseService).updateCourse(
                anyLong(), anyString(), anyString(), nullable(String.class), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("강좌를 작성한 강사 본인이 공개 요청을 하면 200을 반환한다")
    void givenOwnerInstructor_whenPublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/publish")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(courseService).publishCourse(1L, 1L, false);
    }

    @Test
    @DisplayName("어드민이 강좌 공개 요청을 하면 소유자가 아니어도 200을 반환한다")
    void givenAdmin_whenPublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/publish")
                        .with(authentication(adminAuthentication(99L)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(courseService).publishCourse(1L, 99L, true);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 공개 요청을 하면 403을 반환한다")
    void givenOtherInstructor_whenPublishCourse_thenReturns403() throws Exception {
        doThrow(new CourseAccessDeniedException("작성한 강사만 접근할 수 있습니다."))
                .when(courseService).publishCourse(1L, 2L, false);

        mockMvc.perform(post("/api/courses/1/publish")
                        .with(authentication(instructorAuthentication(2L)))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("작성한 강사만 접근할 수 있습니다."));

        verify(courseService).publishCourse(1L, 2L, false);
    }

    @Test
    @DisplayName("강좌 공개 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenPublishCourse_thenReturns400() throws Exception {
        doThrow(new CourseException("강의와 미션을 1개 이상 포함해야 공개할 수 있습니다."))
                .when(courseService).publishCourse(anyLong(), anyLong(), anyBoolean());

        mockMvc.perform(post("/api/courses/1/publish")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("강의와 미션을 1개 이상 포함해야 공개할 수 있습니다."));

        verify(courseService).publishCourse(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("강좌를 작성한 강사 본인이 비공개 요청을 하면 200을 반환한다")
    void givenOwnerInstructor_whenUnpublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/unpublish")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(courseService).unpublishCourse(1L, 1L, false);
    }

    @Test
    @DisplayName("어드민이 강좌 비공개 요청을 하면 소유자가 아니어도 200을 반환한다")
    void givenAdmin_whenUnpublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/unpublish")
                        .with(authentication(adminAuthentication(99L)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(courseService).unpublishCourse(1L, 99L, true);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 비공개 요청을 하면 403을 반환한다")
    void givenOtherInstructor_whenUnpublishCourse_thenReturns403() throws Exception {
        doThrow(new CourseAccessDeniedException("작성한 강사만 접근할 수 있습니다."))
                .when(courseService).unpublishCourse(1L, 2L, false);

        mockMvc.perform(post("/api/courses/1/unpublish")
                        .with(authentication(instructorAuthentication(2L)))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("작성한 강사만 접근할 수 있습니다."));

        verify(courseService).unpublishCourse(1L, 2L, false);
    }

    @Test
    @DisplayName("강좌 비공개 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenUnpublishCourse_thenReturns400() throws Exception {
        doThrow(new CourseException("접근 권한이 없습니다."))
                .when(courseService).unpublishCourse(anyLong(), anyLong(), anyBoolean());

        mockMvc.perform(post("/api/courses/1/unpublish")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));

        verify(courseService).unpublishCourse(anyLong(), anyLong(), anyBoolean());
    }

    // --- deleteCourse ---

    @Test
    @DisplayName("강좌를 작성한 강사 본인이 삭제 요청을 하면 200을 반환한다")
    void givenOwnerInstructor_whenDeleteCourse_thenReturns200() throws Exception {
        mockMvc.perform(delete("/api/courses/1")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(courseService).deleteCourse(1L, 1L, false);
    }

    @Test
    @DisplayName("어드민이 강좌 삭제 요청을 하면 소유자가 아니어도 200을 반환한다")
    void givenAdmin_whenDeleteCourse_thenReturns200() throws Exception {
        mockMvc.perform(delete("/api/courses/1")
                        .with(authentication(adminAuthentication(99L)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(courseService).deleteCourse(1L, 99L, true);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 삭제 요청을 하면 403을 반환한다")
    void givenOtherInstructor_whenDeleteCourse_thenReturns403() throws Exception {
        doThrow(new CourseAccessDeniedException("작성한 강사만 접근할 수 있습니다."))
                .when(courseService).deleteCourse(1L, 2L, false);

        mockMvc.perform(delete("/api/courses/1")
                        .with(authentication(instructorAuthentication(2L)))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("작성한 강사만 접근할 수 있습니다."));

        verify(courseService).deleteCourse(1L, 2L, false);
    }

    @Test
    @DisplayName("강좌 삭제 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenDeleteCourse_thenReturns400() throws Exception {
        doThrow(new CourseException("삭제된 강좌는 수정할 수 없습니다."))
                .when(courseService).deleteCourse(anyLong(), anyLong(), anyBoolean());

        mockMvc.perform(delete("/api/courses/1")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("삭제된 강좌는 수정할 수 없습니다."));

        verify(courseService).deleteCourse(anyLong(), anyLong(), anyBoolean());
    }

    // --- deleteLecture ---

    @Test
    @DisplayName("강좌를 작성한 강사 본인이 강의 삭제 요청을 하면 200을 반환한다")
    void givenOwnerInstructor_whenDeleteLecture_thenReturns200() throws Exception {
        mockMvc.perform(delete("/api/courses/1/lectures/10")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(courseService).deleteLecture(1L, 10L, 1L, false);
    }

    @Test
    @DisplayName("어드민이 강의 삭제 요청을 하면 소유자가 아니어도 200을 반환한다")
    void givenAdmin_whenDeleteLecture_thenReturns200() throws Exception {
        mockMvc.perform(delete("/api/courses/1/lectures/10")
                        .with(authentication(adminAuthentication(99L)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(courseService).deleteLecture(1L, 10L, 99L, true);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 강의 삭제 요청을 하면 403을 반환한다")
    void givenOtherInstructor_whenDeleteLecture_thenReturns403() throws Exception {
        doThrow(new CourseAccessDeniedException("작성한 강사만 접근할 수 있습니다."))
                .when(courseService).deleteLecture(1L, 10L, 2L, false);

        mockMvc.perform(delete("/api/courses/1/lectures/10")
                        .with(authentication(instructorAuthentication(2L)))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("작성한 강사만 접근할 수 있습니다."));

        verify(courseService).deleteLecture(1L, 10L, 2L, false);
    }

    @Test
    @DisplayName("강의 삭제 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenDeleteLecture_thenReturns400() throws Exception {
        doThrow(new CourseException("강의를 찾을 수 없습니다."))
                .when(courseService).deleteLecture(anyLong(), anyLong(), anyLong(), anyBoolean());

        mockMvc.perform(delete("/api/courses/1/lectures/10")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("강의를 찾을 수 없습니다."));

        verify(courseService).deleteLecture(anyLong(), anyLong(), anyLong(), anyBoolean());
    }

    // --- deleteMission ---

    @Test
    @DisplayName("강좌를 작성한 강사 본인이 미션 삭제 요청을 하면 200을 반환한다")
    void givenOwnerInstructor_whenDeleteMission_thenReturns200() throws Exception {
        mockMvc.perform(delete("/api/courses/1/missions/20")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(courseService).deleteMission(1L, 20L, 1L, false);
    }

    @Test
    @DisplayName("어드민이 미션 삭제 요청을 하면 소유자가 아니어도 200을 반환한다")
    void givenAdmin_whenDeleteMission_thenReturns200() throws Exception {
        mockMvc.perform(delete("/api/courses/1/missions/20")
                        .with(authentication(adminAuthentication(99L)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(courseService).deleteMission(1L, 20L, 99L, true);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 미션 삭제 요청을 하면 403을 반환한다")
    void givenOtherInstructor_whenDeleteMission_thenReturns403() throws Exception {
        doThrow(new CourseAccessDeniedException("작성한 강사만 접근할 수 있습니다."))
                .when(courseService).deleteMission(1L, 20L, 2L, false);

        mockMvc.perform(delete("/api/courses/1/missions/20")
                        .with(authentication(instructorAuthentication(2L)))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("작성한 강사만 접근할 수 있습니다."));

        verify(courseService).deleteMission(1L, 20L, 2L, false);
    }

    @Test
    @DisplayName("미션 삭제 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenDeleteMission_thenReturns400() throws Exception {
        doThrow(new CourseException("미션을 찾을 수 없습니다."))
                .when(courseService).deleteMission(anyLong(), anyLong(), anyLong(), anyBoolean());

        mockMvc.perform(delete("/api/courses/1/missions/20")
                        .with(authentication(instructorAuthentication(1L)))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("미션을 찾을 수 없습니다."));

        verify(courseService).deleteMission(anyLong(), anyLong(), anyLong(), anyBoolean());
    }

    // --- createCourse validation ---

    @Test
    @WithMockUser
    @DisplayName("강좌 생성 시 제목이 빈 값이면 400을 반환한다")
    void givenBlankTitle_whenCreateCourse_thenReturns400() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("", "강좌 설명", null);

        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("강좌 생성 시 제목이 100자를 초과하면 400을 반환한다")
    void givenTitleExceeding100Chars_whenCreateCourse_thenReturns400() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("가".repeat(101), "강좌 설명", null);

        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    // --- updateCourse validation ---

    @Test
    @WithMockUser
    @DisplayName("강좌 수정 시 제목이 빈 값이면 400을 반환한다")
    void givenBlankTitle_whenUpdateCourse_thenReturns400() throws Exception {
        UpdateCourseRequest request = new UpdateCourseRequest("", "수정된 설명", null);

        mockMvc.perform(patch("/api/courses/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("강좌 수정 시 제목이 100자를 초과하면 400을 반환한다")
    void givenTitleExceeding100Chars_whenUpdateCourse_thenReturns400() throws Exception {
        UpdateCourseRequest request = new UpdateCourseRequest("가".repeat(101), "수정된 설명", null);

        mockMvc.perform(patch("/api/courses/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    // --- createCourse description validation ---

    @Test
    @WithMockUser
    @DisplayName("강좌 생성 시 설명이 빈 값이면 400을 반환한다")
    void givenBlankDescription_whenCreateCourse_thenReturns400() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("강좌 제목", "", null);

        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("강좌 생성 시 설명이 4096자를 초과하면 400을 반환한다")
    void givenDescriptionExceeding4096Chars_whenCreateCourse_thenReturns400() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("강좌 제목", "가".repeat(4097), null);

        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    // --- updateCourse description validation ---

    @Test
    @WithMockUser
    @DisplayName("강좌 수정 시 설명이 빈 값이면 400을 반환한다")
    void givenBlankDescription_whenUpdateCourse_thenReturns400() throws Exception {
        UpdateCourseRequest request = new UpdateCourseRequest("수정된 제목", "", null);

        mockMvc.perform(patch("/api/courses/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("강좌 수정 시 설명이 4096자를 초과하면 400을 반환한다")
    void givenDescriptionExceeding4096Chars_whenUpdateCourse_thenReturns400() throws Exception {
        UpdateCourseRequest request = new UpdateCourseRequest("수정된 제목", "가".repeat(4097), null);

        mockMvc.perform(patch("/api/courses/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    // --- addLecture validation ---

    @Test
    @WithMockUser
    @DisplayName("강의 추가 시 제목이 빈 값이면 400을 반환한다")
    void givenBlankTitle_whenAddLecture_thenReturns400() throws Exception {
        AddLectureRequest request = new AddLectureRequest("", "https://example.com/lecture", "mp4");

        mockMvc.perform(post("/api/courses/1/lectures")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("강의 추가 시 제목이 100자를 초과하면 400을 반환한다")
    void givenTitleExceeding100Chars_whenAddLecture_thenReturns400() throws Exception {
        AddLectureRequest request = new AddLectureRequest("가".repeat(101), "https://example.com/lecture", "mp4");

        mockMvc.perform(post("/api/courses/1/lectures")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("강의 추가 시 자료 URL이 빈 값이면 400을 반환한다")
    void givenBlankContentUrl_whenAddLecture_thenReturns400() throws Exception {
        AddLectureRequest request = new AddLectureRequest("강의 제목", "", "mp4");

        mockMvc.perform(post("/api/courses/1/lectures")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("강의 추가 요청이 성공하면 201을 반환한다")
    void givenValidRequest_whenAddLecture_thenReturns201() throws Exception {
        AddLectureRequest request = new AddLectureRequest("강의 제목", "https://example.com/lecture", "mp4");

        mockMvc.perform(post("/api/courses/1/lectures")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(courseService).addLecture(1L, "강의 제목", "https://example.com/lecture", "mp4");
    }

    @Test
    @WithMockUser
    @DisplayName("강의 추가 시 자료 타입이 null이면 400을 반환한다")
    void givenNullContentType_whenAddLecture_thenReturns400() throws Exception {
        AddLectureRequest request = new AddLectureRequest("강의 제목", "https://example.com/lecture", null);

        mockMvc.perform(post("/api/courses/1/lectures")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("강의 추가 시 자료 타입이 빈 값이면 400을 반환한다")
    void givenBlankContentType_whenAddLecture_thenReturns400() throws Exception {
        AddLectureRequest request = new AddLectureRequest("강의 제목", "https://example.com/lecture", "");

        mockMvc.perform(post("/api/courses/1/lectures")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    // --- addMission validation ---

    @Test
    @WithMockUser
    @DisplayName("미션 추가 시 제목이 빈 값이면 400을 반환한다")
    void givenBlankTitle_whenAddMission_thenReturns400() throws Exception {
        AddMissionRequest request = new AddMissionRequest("", "미션 내용");

        mockMvc.perform(post("/api/courses/1/missions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("미션 추가 시 제목이 100자를 초과하면 400을 반환한다")
    void givenTitleExceeding100Chars_whenAddMission_thenReturns400() throws Exception {
        AddMissionRequest request = new AddMissionRequest("가".repeat(101), "미션 내용");

        mockMvc.perform(post("/api/courses/1/missions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("미션 추가 시 문제 내용이 빈 값이면 400을 반환한다")
    void givenBlankContent_whenAddMission_thenReturns400() throws Exception {
        AddMissionRequest request = new AddMissionRequest("미션 제목", "");

        mockMvc.perform(post("/api/courses/1/missions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("미션 추가 시 문제 내용이 4096자를 초과하면 400을 반환한다")
    void givenContentExceeding4096Chars_whenAddMission_thenReturns400() throws Exception {
        AddMissionRequest request = new AddMissionRequest("미션 제목", "가".repeat(4097));

        mockMvc.perform(post("/api/courses/1/missions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    // --- reorderItems ---

    @Test
    @WithMockUser
    @DisplayName("순서 변경 요청이 성공하면 200을 반환하고 요청 순서대로 서비스에 전달된다")
    void givenValidRequest_whenReorderItems_thenReturns200() throws Exception {
        ReorderRequest request = new ReorderRequest(List.of(
                new ReorderRequest.Item(ReorderRequest.Type.MISSION, 20L),
                new ReorderRequest.Item(ReorderRequest.Type.LECTURE, 10L)));

        mockMvc.perform(patch("/api/courses/1/reorder")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(courseService).reorderItems(1L, List.of("MISSION", "LECTURE"), List.of(20L, 10L));
    }

    @Test
    @WithMockUser
    @DisplayName("순서 변경 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenReorderItems_thenReturns400() throws Exception {
        doThrow(new CourseException("강의를 찾을 수 없습니다."))
                .when(courseService).reorderItems(anyLong(), anyList(), anyList());

        ReorderRequest request = new ReorderRequest(List.of(
                new ReorderRequest.Item(ReorderRequest.Type.LECTURE, 999L)));

        mockMvc.perform(patch("/api/courses/1/reorder")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("강의를 찾을 수 없습니다."));

        verify(courseService).reorderItems(anyLong(), anyList(), anyList());
    }

    @Test
    @WithMockUser
    @DisplayName("순서 변경 요청의 항목 목록이 비어있으면 400을 반환한다")
    void givenEmptyItems_whenReorderItems_thenReturns400() throws Exception {
        ReorderRequest request = new ReorderRequest(List.of());

        mockMvc.perform(patch("/api/courses/1/reorder")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("순서 변경 요청의 항목에 id가 없으면 400을 반환한다")
    void givenNullItemId_whenReorderItems_thenReturns400() throws Exception {
        String requestBody = "{\"items\":[{\"type\":\"LECTURE\",\"id\":null}]}";

        mockMvc.perform(patch("/api/courses/1/reorder")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithMockUser
    @DisplayName("순서 변경 요청의 항목 타입 값이 잘못되면 400을 반환한다")
    void givenInvalidItemType_whenReorderItems_thenReturns400() throws Exception {
        String requestBody = "{\"items\":[{\"type\":\"INVALID\",\"id\":10}]}";

        mockMvc.perform(patch("/api/courses/1/reorder")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }
}
