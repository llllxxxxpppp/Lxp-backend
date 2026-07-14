package com.lcs.lxp.security.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.lxp.course.dto.request.AddLectureRequest;
import com.lcs.lxp.course.dto.request.AddMissionRequest;
import com.lcs.lxp.course.dto.request.CreateCourseRequest;
import com.lcs.lxp.course.dto.request.ReorderRequest;
import com.lcs.lxp.course.dto.request.UpdateCourseRequest;
import com.lcs.lxp.course.model.entity.Course;
import com.lcs.lxp.course.model.entity.Lecture;
import com.lcs.lxp.course.model.entity.Mission;
import com.lcs.lxp.course.model.vo.InstructorId;
import com.lcs.lxp.course.model.vo.Title;
import com.lcs.lxp.course.repository.CourseRepository;
import com.lcs.lxp.member.model.entity.InstructorMember;
import com.lcs.lxp.member.repository.MemberRepository;
import com.lcs.lxp.security.principal.CustomUserPrincipal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * COURSE-08b: 정지된 강사 2차 방어 인터셉터(AOP).
 *
 * <p>{@code CourseService}의 강좌/강의/미션 생성·수정·공개/비공개 메서드에 부착된
 * {@code @RejectSuspendedInstructor} 애노테이션이 실제로 스프링 AOP 프록시를 통해
 * 인터셉트되어 동작하는지를 검증하는 통합 테스트이다.
 *
 * <p>{@code MemberRepository}만 {@link MockitoBean}으로 대체하고, {@code CourseService},
 * {@code CourseController}, {@code CourseRepository}(H2 실제 빈), {@code SecurityConfig} 등
 * 나머지는 모두 실제 스프링 빈을 사용한다 — 이를 통해 애스펙트가 실제 프록시 체인에
 * 위빙(weaving)되어 있는지, 그리고 예외가 {@code CourseExceptionHandler}를 거쳐 실제로
 * 403 Forbidden 응답으로 변환되는지까지 end-to-end로 확인한다.
 *
 * <p>인증 정보는 {@code @WithMockUser}가 생성하는 일반 {@code UserDetails} 대신,
 * 애스펙트가 필요로 하는 {@link CustomUserPrincipal}을 직접 담은 {@link Authentication}을
 * {@code SecurityMockMvcRequestPostProcessors.authentication(...)}으로 주입하여 사용한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SuspendedInstructorAspectIntegrationTest {

    private static final long SUSPENDED_INSTRUCTOR_ID = 100L;
    private static final long ACTIVE_INSTRUCTOR_ID = 200L;
    private static final long ADMIN_ID = 300L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @MockitoBean
    private MemberRepository memberRepository;

    private Long seededCourseId;
    private Long seededLectureId;
    private Long seededMissionId;

    @BeforeEach
    void setUp() {
        Course course = Course.create(new InstructorId(ACTIVE_INSTRUCTOR_ID), new Title("강좌 제목"), "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의 제목"), "/lectures/1", "mp4");
        Mission mission = course.addMission(new Title("미션 제목"), "문제 내용");
        Course saved = courseRepository.save(course);

        seededCourseId = saved.getId().value();
        seededLectureId = lecture.getId().value();
        seededMissionId = mission.getId().value();
    }

    // --- POST /api/courses (createCourse) ---

    @Test
    @DisplayName("정지된 강사가 강좌 생성을 요청하면 403 Forbidden을 반환한다")
    void givenSuspendedInstructor_whenCreateCourse_thenReturns403() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("새 강좌", "설명", null);

        mockMvc.perform(post("/api/courses")
                        .with(authentication(suspendedInstructorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(memberRepository).findById(SUSPENDED_INSTRUCTOR_ID);
    }

    @Test
    @DisplayName("정지되지 않은 강사가 강좌 생성을 요청하면 201 Created를 반환한다")
    void givenActiveInstructor_whenCreateCourse_thenReturns201() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("새 강좌", "설명", null);

        mockMvc.perform(post("/api/courses")
                        .with(authentication(activeInstructorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(memberRepository).findById(ACTIVE_INSTRUCTOR_ID);
    }

    // --- PATCH /api/courses/{courseId} (updateCourse) ---

    @Test
    @DisplayName("정지된 강사가 강좌 수정을 요청하면 403 Forbidden을 반환한다")
    void givenSuspendedInstructor_whenUpdateCourse_thenReturns403() throws Exception {
        UpdateCourseRequest request = new UpdateCourseRequest("수정된 제목", "수정된 설명", null);

        mockMvc.perform(patch("/api/courses/" + seededCourseId)
                        .with(authentication(suspendedInstructorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(memberRepository).findById(SUSPENDED_INSTRUCTOR_ID);
    }

    // --- POST /api/courses/{courseId}/publish (publishCourse) ---

    @Test
    @DisplayName("정지된 강사가 강좌 공개를 요청하면 403 Forbidden을 반환한다")
    void givenSuspendedInstructor_whenPublishCourse_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/" + seededCourseId + "/publish")
                        .with(authentication(suspendedInstructorAuthentication())))
                .andExpect(status().isForbidden());

        verify(memberRepository).findById(SUSPENDED_INSTRUCTOR_ID);
    }

    @Test
    @DisplayName("정지되지 않은 강사가 강좌 공개를 요청하면 200 OK를 반환한다")
    void givenActiveInstructor_whenPublishCourse_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/courses/" + seededCourseId + "/publish")
                        .with(authentication(activeInstructorAuthentication())))
                .andExpect(status().isOk());

        verify(memberRepository).findById(ACTIVE_INSTRUCTOR_ID);
    }

    // --- POST /api/courses/{courseId}/unpublish (unpublishCourse) ---

    @Test
    @DisplayName("정지된 강사가 강좌 비공개를 요청하면 403 Forbidden을 반환한다")
    void givenSuspendedInstructor_whenUnpublishCourse_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/" + seededCourseId + "/unpublish")
                        .with(authentication(suspendedInstructorAuthentication())))
                .andExpect(status().isForbidden());

        verify(memberRepository).findById(SUSPENDED_INSTRUCTOR_ID);
    }

    @Test
    @DisplayName("어드민이 강좌 비공개를 요청하면 강사 정지 여부 조회 없이 200 OK를 반환한다")
    void givenAdminRole_whenUnpublishCourse_thenReturns200WithoutSuspensionCheck() throws Exception {
        mockMvc.perform(post("/api/courses/" + seededCourseId + "/unpublish")
                        .with(authentication(adminAuthentication())))
                .andExpect(status().isOk());

        verifyNoInteractions(memberRepository);
    }

    // --- POST /api/courses/{courseId}/lectures (addLecture) ---

    @Test
    @DisplayName("정지된 강사가 강의 추가를 요청하면 403 Forbidden을 반환한다")
    void givenSuspendedInstructor_whenAddLecture_thenReturns403() throws Exception {
        AddLectureRequest request = new AddLectureRequest("새 강의", "/lectures/2", "mp4");

        mockMvc.perform(post("/api/courses/" + seededCourseId + "/lectures")
                        .with(authentication(suspendedInstructorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(memberRepository).findById(SUSPENDED_INSTRUCTOR_ID);
    }

    @Test
    @DisplayName("정지되지 않은 강사가 강의 추가를 요청하면 201 Created를 반환한다")
    void givenActiveInstructor_whenAddLecture_thenReturns201() throws Exception {
        AddLectureRequest request = new AddLectureRequest("새 강의", "/lectures/2", "mp4");

        mockMvc.perform(post("/api/courses/" + seededCourseId + "/lectures")
                        .with(authentication(activeInstructorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(memberRepository).findById(ACTIVE_INSTRUCTOR_ID);
    }

    // --- POST /api/courses/{courseId}/lectures/{lectureId}/publish (publishLecture) ---

    @Test
    @DisplayName("정지된 강사가 강의 공개를 요청하면 403 Forbidden을 반환한다")
    void givenSuspendedInstructor_whenPublishLecture_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/" + seededCourseId + "/lectures/" + seededLectureId + "/publish")
                        .with(authentication(suspendedInstructorAuthentication())))
                .andExpect(status().isForbidden());

        verify(memberRepository).findById(SUSPENDED_INSTRUCTOR_ID);
    }

    // --- POST /api/courses/{courseId}/lectures/{lectureId}/unpublish (unpublishLecture) ---

    @Test
    @DisplayName("정지된 강사가 강의 비공개를 요청하면 403 Forbidden을 반환한다")
    void givenSuspendedInstructor_whenUnpublishLecture_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/" + seededCourseId + "/lectures/" + seededLectureId + "/unpublish")
                        .with(authentication(suspendedInstructorAuthentication())))
                .andExpect(status().isForbidden());

        verify(memberRepository).findById(SUSPENDED_INSTRUCTOR_ID);
    }

    // --- POST /api/courses/{courseId}/missions (addMission) ---

    @Test
    @DisplayName("정지된 강사가 미션 추가를 요청하면 403 Forbidden을 반환한다")
    void givenSuspendedInstructor_whenAddMission_thenReturns403() throws Exception {
        AddMissionRequest request = new AddMissionRequest("새 미션", "문제 내용");

        mockMvc.perform(post("/api/courses/" + seededCourseId + "/missions")
                        .with(authentication(suspendedInstructorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(memberRepository).findById(SUSPENDED_INSTRUCTOR_ID);
    }

    @Test
    @DisplayName("정지되지 않은 강사가 미션 추가를 요청하면 201 Created를 반환한다")
    void givenActiveInstructor_whenAddMission_thenReturns201() throws Exception {
        AddMissionRequest request = new AddMissionRequest("새 미션", "문제 내용");

        mockMvc.perform(post("/api/courses/" + seededCourseId + "/missions")
                        .with(authentication(activeInstructorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(memberRepository).findById(ACTIVE_INSTRUCTOR_ID);
    }

    // --- POST /api/courses/{courseId}/missions/{missionId}/publish (publishMission) ---

    @Test
    @DisplayName("정지된 강사가 미션 공개를 요청하면 403 Forbidden을 반환한다")
    void givenSuspendedInstructor_whenPublishMission_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/" + seededCourseId + "/missions/" + seededMissionId + "/publish")
                        .with(authentication(suspendedInstructorAuthentication())))
                .andExpect(status().isForbidden());

        verify(memberRepository).findById(SUSPENDED_INSTRUCTOR_ID);
    }

    // --- POST /api/courses/{courseId}/missions/{missionId}/unpublish (unpublishMission) ---

    @Test
    @DisplayName("정지된 강사가 미션 비공개를 요청하면 403 Forbidden을 반환한다")
    void givenSuspendedInstructor_whenUnpublishMission_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/courses/" + seededCourseId + "/missions/" + seededMissionId + "/unpublish")
                        .with(authentication(suspendedInstructorAuthentication())))
                .andExpect(status().isForbidden());

        verify(memberRepository).findById(SUSPENDED_INSTRUCTOR_ID);
    }

    // --- 범위 밖 회귀 테스트: 삭제/순서변경은 정지된 강사도 차단되지 않는다 ---

    @Test
    @DisplayName("정지된 강사가 본인 소유 강좌 삭제를 요청해도 차단되지 않고 200 OK를 반환한다 (범위 밖)")
    void givenSuspendedInstructor_whenDeleteCourse_thenReturns200WithoutSuspensionCheck() throws Exception {
        // COURSE-09 소유권 검증과 충돌하지 않도록, 정지된 강사 본인이 소유한 강좌를 별도로 시드한다.
        Course ownCourse =
                Course.create(new InstructorId(SUSPENDED_INSTRUCTOR_ID), new Title("정지 강사 소유 강좌"), "설명", null);
        Long ownCourseId = courseRepository.save(ownCourse).getId().value();

        mockMvc.perform(delete("/api/courses/" + ownCourseId)
                        .with(authentication(suspendedInstructorAuthentication())))
                .andExpect(status().isOk());

        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("정지된 강사가 순서 변경을 요청해도 차단되지 않고 200 OK를 반환한다 (범위 밖)")
    void givenSuspendedInstructor_whenReorderItems_thenReturns200WithoutSuspensionCheck() throws Exception {
        ReorderRequest request = new ReorderRequest(List.of(
                new ReorderRequest.Item(ReorderRequest.Type.MISSION, seededMissionId),
                new ReorderRequest.Item(ReorderRequest.Type.LECTURE, seededLectureId)));

        mockMvc.perform(patch("/api/courses/" + seededCourseId + "/reorder")
                        .with(authentication(suspendedInstructorAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verifyNoInteractions(memberRepository);
    }

    private Authentication suspendedInstructorAuthentication() {
        InstructorMember suspended =
                InstructorMember.create("suspended@test.com", "encoded_password", "이름", null, "소개");
        ReflectionTestUtils.setField(suspended, "id", SUSPENDED_INSTRUCTOR_ID);
        suspended.suspend();
        when(memberRepository.findById(SUSPENDED_INSTRUCTOR_ID)).thenReturn(Optional.of(suspended));
        return buildAuthentication(SUSPENDED_INSTRUCTOR_ID, "ROLE_INSTRUCTOR");
    }

    private Authentication activeInstructorAuthentication() {
        InstructorMember active =
                InstructorMember.create("active@test.com", "encoded_password", "이름", null, "소개");
        ReflectionTestUtils.setField(active, "id", ACTIVE_INSTRUCTOR_ID);
        when(memberRepository.findById(ACTIVE_INSTRUCTOR_ID)).thenReturn(Optional.of(active));
        return buildAuthentication(ACTIVE_INSTRUCTOR_ID, "ROLE_INSTRUCTOR");
    }

    private Authentication adminAuthentication() {
        return buildAuthentication(ADMIN_ID, "ROLE_ADMIN");
    }

    private Authentication buildAuthentication(long userId, String role) {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                userId, "user" + userId + "@test.com", "", List.of(new SimpleGrantedAuthority(role)), false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }
}
