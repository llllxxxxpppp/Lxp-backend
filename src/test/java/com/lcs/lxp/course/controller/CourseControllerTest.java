package com.lcs.lxp.course.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.lxp.course.dto.request.CreateCourseRequest;
import com.lcs.lxp.course.dto.request.UpdateCourseRequest;
import com.lcs.lxp.course.dto.response.CourseDetailResponse;
import com.lcs.lxp.course.dto.response.CourseSummaryResponse;
import com.lcs.lxp.course.dto.response.LectureResponse;
import com.lcs.lxp.course.dto.response.MissionResponse;
import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.service.CourseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    private CourseService courseService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- getCourseSummary ---

    @Test
    @WithMockUser
    @DisplayName("강좌 요약 조회 요청이 성공하면 200과 요약 정보를 반환한다")
    void givenExistingCourse_whenGetCourseSummary_thenReturns200WithSummary() throws Exception {
        CourseSummaryResponse summary = new CourseSummaryResponse(1L, 1L, "강좌 제목", "PRIVATE");
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
        List<LectureResponse> lectures = List.of(new LectureResponse(10L, "강의 제목", "PRIVATE"));
        List<MissionResponse> missions = List.of(new MissionResponse(20L, "미션 제목", "PRIVATE"));
        CourseDetailResponse detail = new CourseDetailResponse(1L, 1L, "강좌 제목", "PRIVATE", lectures, missions);
        when(courseService.getCourseDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/courses/1/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(1L))
                .andExpect(jsonPath("$.lectures[0].lectureId").value(10L))
                .andExpect(jsonPath("$.lectures[0].title").value("강의 제목"))
                .andExpect(jsonPath("$.missions[0].missionId").value(20L))
                .andExpect(jsonPath("$.missions[0].title").value("미션 제목"));

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
    @WithMockUser(username = "1")
    @DisplayName("강좌 생성 요청이 성공하면 201을 반환한다")
    void givenValidRequest_whenCreateCourse_thenReturns201() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("강좌 제목");

        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(courseService).createCourse(1L, "강좌 제목");
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("강좌 생성 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenCreateCourse_thenReturns400() throws Exception {
        doThrow(new CourseException("접근 권한이 없습니다."))
                .when(courseService).createCourse(anyLong(), anyString());

        CreateCourseRequest request = new CreateCourseRequest("강좌 제목");

        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));

        verify(courseService).createCourse(anyLong(), anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("강좌 수정 요청이 성공하면 200을 반환한다")
    void givenValidRequest_whenUpdateCourse_thenReturns200() throws Exception {
        UpdateCourseRequest request = new UpdateCourseRequest("수정된 제목");

        mockMvc.perform(patch("/api/courses/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(courseService).updateCourse(1L, "수정된 제목");
    }

    @Test
    @WithMockUser
    @DisplayName("강좌 수정 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenUpdateCourse_thenReturns400() throws Exception {
        doThrow(new CourseException("공개 상태에서는 강좌를 수정할 수 없습니다."))
                .when(courseService).updateCourse(anyLong(), anyString());

        UpdateCourseRequest request = new UpdateCourseRequest("수정된 제목");

        mockMvc.perform(patch("/api/courses/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("공개 상태에서는 강좌를 수정할 수 없습니다."));

        verify(courseService).updateCourse(anyLong(), anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("강좌 공개 요청이 성공하면 200을 반환한다")
    void givenValidRequest_whenPublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/publish")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(courseService).publishCourse(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("강좌 공개 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenPublishCourse_thenReturns400() throws Exception {
        doThrow(new CourseException("강의와 미션을 1개 이상 포함해야 공개할 수 있습니다."))
                .when(courseService).publishCourse(anyLong());

        mockMvc.perform(post("/api/courses/1/publish")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("강의와 미션을 1개 이상 포함해야 공개할 수 있습니다."));

        verify(courseService).publishCourse(anyLong());
    }

    @Test
    @WithMockUser
    @DisplayName("강좌 비공개 요청이 성공하면 200을 반환한다")
    void givenValidRequest_whenUnpublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/1/unpublish")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(courseService).unpublishCourse(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("강좌 비공개 시 서비스에서 예외가 발생하면 400을 반환한다")
    void givenServiceException_whenUnpublishCourse_thenReturns400() throws Exception {
        doThrow(new CourseException("접근 권한이 없습니다."))
                .when(courseService).unpublishCourse(anyLong());

        mockMvc.perform(post("/api/courses/1/unpublish")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));

        verify(courseService).unpublishCourse(anyLong());
    }
}
