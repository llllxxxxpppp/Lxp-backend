package com.lcs.lxp.course.service;

import com.lcs.lxp.course.dto.response.CourseDetailResponse;
import com.lcs.lxp.course.dto.response.CourseSummaryResponse;
import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.model.entity.Course;
import com.lcs.lxp.course.model.vo.ContentStatus;
import com.lcs.lxp.course.model.vo.InstructorId;
import com.lcs.lxp.course.model.vo.Title;
import com.lcs.lxp.course.repository.CourseRepository;
import com.lcs.lxp.member.model.MemberRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    private Course privateCourse;
    private Course publishedCourse;

    @BeforeEach
    void setUp() {
        privateCourse = Course.create(new InstructorId(1L), new Title("강좌 제목"));
        privateCourse.addLecture(new Title("강의"));
        privateCourse.addMission(new Title("미션"), "문제 내용");

        publishedCourse = Course.create(new InstructorId(1L), new Title("강좌 제목"));
        publishedCourse.addLecture(new Title("강의"));
        publishedCourse.addMission(new Title("미션"), "문제 내용");
        publishedCourse.publish();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(MemberRole role) {
        var authority = new SimpleGrantedAuthority("ROLE_" + role.name());
        var auth = new UsernamePasswordAuthenticationToken("user", null, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // --- getCourseSummary ---

    @Test
    @DisplayName("강좌 요약 정보를 조회하면 강좌 기본 정보를 반환한다")
    void givenExistingCourse_whenGetCourseSummary_thenReturnsSummary() {
        Course course = Course.create(new InstructorId(1L), new Title("강좌 제목"));
        ReflectionTestUtils.setField(course, "id", 1L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        CourseSummaryResponse summary = courseService.getCourseSummary(1L);

        assertEquals(1L, summary.courseId());
        assertEquals(1L, summary.instructorId());
        assertEquals("강좌 제목", summary.title());
        assertEquals(ContentStatus.PRIVATE.name(), summary.status());
    }

    @Test
    @DisplayName("존재하지 않는 강좌 요약 조회 시 예외가 발생한다")
    void givenNonExistentCourse_whenGetCourseSummary_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.getCourseSummary(999L));
    }

    // --- getCourseDetail ---

    @Test
    @DisplayName("강좌 상세 정보를 조회하면 강의와 미션을 포함한 정보를 반환한다")
    void givenExistingCourseWithLecturesAndMissions_whenGetCourseDetail_thenReturnsDetail() {
        Course course = Course.create(new InstructorId(1L), new Title("강좌 제목"));
        ReflectionTestUtils.setField(course, "id", 1L);
        ReflectionTestUtils.setField(course.addLecture(new Title("강의 제목")), "id", 10L);
        ReflectionTestUtils.setField(course.addMission(new Title("미션 제목"), "문제 내용"), "id", 20L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        CourseDetailResponse detail = courseService.getCourseDetail(1L);

        assertEquals(1L, detail.courseId());
        assertEquals(1L, detail.instructorId());
        assertEquals("강좌 제목", detail.title());
        assertEquals(ContentStatus.PRIVATE.name(), detail.status());
        assertEquals(1, detail.lectures().size());
        assertEquals(10L, detail.lectures().get(0).lectureId());
        assertEquals("강의 제목", detail.lectures().get(0).title());
        assertEquals(1, detail.missions().size());
        assertEquals(20L, detail.missions().get(0).missionId());
        assertEquals("미션 제목", detail.missions().get(0).title());
    }

    @Test
    @DisplayName("존재하지 않는 강좌 상세 조회 시 예외가 발생한다")
    void givenNonExistentCourse_whenGetCourseDetail_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.getCourseDetail(999L));
    }

    // --- createCourse ---

    @Test
    @DisplayName("강사가 강좌를 생성하면 저장된다")
    void givenInstructorRole_whenCreateCourse_thenCourseIsSaved() {
        authenticate(MemberRole.INSTRUCTOR);

        courseService.createCourse(1L, "강좌 제목");

        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("일반 회원이 강좌를 생성하면 예외가 발생한다")
    void givenMemberRole_whenCreateCourse_thenThrowsException() {
        authenticate(MemberRole.MEMBER);

        assertThrows(CourseException.class, () -> courseService.createCourse(1L, "강좌 제목"));
    }

    @Test
    @DisplayName("어드민이 강좌를 생성하면 예외가 발생한다")
    void givenAdminRole_whenCreateCourse_thenThrowsException() {
        authenticate(MemberRole.ADMIN);

        assertThrows(CourseException.class, () -> courseService.createCourse(1L, "강좌 제목"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 강좌를 생성하면 예외가 발생한다")
    void givenUnauthenticated_whenCreateCourse_thenThrowsException() {
        assertThrows(CourseException.class, () -> courseService.createCourse(1L, "강좌 제목"));
    }

    // --- updateCourse ---

    @Test
    @DisplayName("비공개 강좌의 제목을 수정하면 제목이 변경된다")
    void givenPrivateCourse_whenUpdateCourse_thenTitleIsUpdated() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.updateCourse(1L, "수정된 제목");

        assertEquals("수정된 제목", privateCourse.getTitle().getValue());
    }

    @Test
    @DisplayName("공개 강좌의 제목을 수정하면 예외가 발생한다")
    void givenPublicCourse_whenUpdateCourse_thenThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(publishedCourse));

        assertThrows(CourseException.class, () -> courseService.updateCourse(1L, "수정된 제목"));
    }

    @Test
    @DisplayName("존재하지 않는 강좌를 수정하면 예외가 발생한다")
    void givenNonExistentCourse_whenUpdateCourse_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.updateCourse(999L, "수정된 제목"));
    }

    // --- publishCourse ---

    @Test
    @DisplayName("강사가 강좌를 공개하면 상태가 PUBLIC이 된다")
    void givenInstructorRole_whenPublishCourse_thenStatusIsPublic() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.publishCourse(1L);

        assertEquals(ContentStatus.PUBLIC, privateCourse.getStatus());
    }

    @Test
    @DisplayName("일반 회원이 강좌를 공개하면 예외가 발생한다")
    void givenMemberRole_whenPublishCourse_thenThrowsException() {
        authenticate(MemberRole.MEMBER);

        assertThrows(CourseException.class, () -> courseService.publishCourse(1L));
    }

    @Test
    @DisplayName("어드민이 강좌를 공개하면 예외가 발생한다")
    void givenAdminRole_whenPublishCourse_thenThrowsException() {
        authenticate(MemberRole.ADMIN);

        assertThrows(CourseException.class, () -> courseService.publishCourse(1L));
    }

    @Test
    @DisplayName("존재하지 않는 강좌를 공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenPublishCourse_thenThrowsException() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.publishCourse(999L));
    }

    // --- unpublishCourse ---

    @Test
    @DisplayName("강사가 강좌를 비공개하면 상태가 PRIVATE이고 삭제 상태가 된다")
    void givenInstructorRole_whenUnpublishCourse_thenStatusIsPrivateAndDeleted() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(publishedCourse));

        courseService.unpublishCourse(1L);

        assertEquals(ContentStatus.PRIVATE, publishedCourse.getStatus());
        assertTrue(publishedCourse.isDeleted());
    }

    @Test
    @DisplayName("어드민이 강좌를 비공개하면 상태가 PRIVATE이고 삭제 상태가 된다")
    void givenAdminRole_whenUnpublishCourse_thenStatusIsPrivateAndDeleted() {
        authenticate(MemberRole.ADMIN);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(publishedCourse));

        courseService.unpublishCourse(1L);

        assertEquals(ContentStatus.PRIVATE, publishedCourse.getStatus());
        assertTrue(publishedCourse.isDeleted());
    }

    @Test
    @DisplayName("일반 회원이 강좌를 비공개하면 예외가 발생한다")
    void givenMemberRole_whenUnpublishCourse_thenThrowsException() {
        authenticate(MemberRole.MEMBER);

        assertThrows(CourseException.class, () -> courseService.unpublishCourse(1L));
    }

    @Test
    @DisplayName("존재하지 않는 강좌를 비공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenUnpublishCourse_thenThrowsException() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.unpublishCourse(999L));
    }
}
