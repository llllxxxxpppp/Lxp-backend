package com.lcs.lxp.course.service;

import com.lcs.lxp.course.dto.response.CourseDetailResponse;
import com.lcs.lxp.course.dto.response.CourseSummaryResponse;
import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.model.entity.Course;
import com.lcs.lxp.course.model.entity.Lecture;
import com.lcs.lxp.course.model.entity.Mission;
import com.lcs.lxp.course.model.vo.ContentStatus;
import com.lcs.lxp.course.model.vo.InstructorId;
import com.lcs.lxp.course.model.vo.LectureId;
import com.lcs.lxp.course.model.vo.MissionId;
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
        privateCourse = Course.create(new InstructorId(1L), new Title("강좌 제목"), "강좌 설명", null);
        Lecture privateLecture = privateCourse.addLecture(new Title("강의"), "/lectures/1");
        ReflectionTestUtils.setField(privateLecture, "id", 10L);
        Mission privateMission = privateCourse.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(privateMission, "id", 20L);

        publishedCourse = Course.create(new InstructorId(1L), new Title("강좌 제목"), "강좌 설명", null);
        Lecture publishedLecture = publishedCourse.addLecture(new Title("강의"), "/lectures/1");
        ReflectionTestUtils.setField(publishedLecture, "id", 10L);
        Mission publishedMission = publishedCourse.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(publishedMission, "id", 20L);
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
        Course course = Course.create(new InstructorId(1L), new Title("강좌 제목"), "강좌 설명", null);
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
        Course course = Course.create(new InstructorId(1L), new Title("강좌 제목"), "강좌 설명", null);
        ReflectionTestUtils.setField(course, "id", 1L);
        ReflectionTestUtils.setField(course.addLecture(new Title("강의 제목"), "/lectures/1"), "id", 10L);
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

        courseService.createCourse(1L, "강좌 제목", "강좌 설명", null);

        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("일반 회원이 강좌를 생성하면 예외가 발생한다")
    void givenMemberRole_whenCreateCourse_thenThrowsException() {
        authenticate(MemberRole.MEMBER);

        assertThrows(CourseException.class, () -> courseService.createCourse(1L, "강좌 제목", "강좌 설명", null));
    }

    @Test
    @DisplayName("어드민이 강좌를 생성하면 예외가 발생한다")
    void givenAdminRole_whenCreateCourse_thenThrowsException() {
        authenticate(MemberRole.ADMIN);

        assertThrows(CourseException.class, () -> courseService.createCourse(1L, "강좌 제목", "강좌 설명", null));
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 강좌를 생성하면 예외가 발생한다")
    void givenUnauthenticated_whenCreateCourse_thenThrowsException() {
        assertThrows(CourseException.class, () -> courseService.createCourse(1L, "강좌 제목", "강좌 설명", null));
    }

    // --- updateCourse ---

    @Test
    @DisplayName("비공개 강좌의 제목을 수정하면 제목이 변경된다")
    void givenPrivateCourse_whenUpdateCourse_thenTitleIsUpdated() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.updateCourse(1L, "수정된 제목", "수정된 설명", null);

        assertEquals("수정된 제목", privateCourse.getTitle().getValue());
    }

    @Test
    @DisplayName("공개 강좌의 제목을 수정하면 예외가 발생한다")
    void givenPublicCourse_whenUpdateCourse_thenThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(publishedCourse));

        assertThrows(CourseException.class, () -> courseService.updateCourse(1L, "수정된 제목", "수정된 설명", null));
    }

    @Test
    @DisplayName("존재하지 않는 강좌를 수정하면 예외가 발생한다")
    void givenNonExistentCourse_whenUpdateCourse_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.updateCourse(999L, "수정된 제목", "수정된 설명", null));
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
    @DisplayName("강사가 강좌를 비공개하면 상태가 PRIVATE이 된다")
    void givenInstructorRole_whenUnpublishCourse_thenStatusIsPrivate() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(publishedCourse));

        courseService.unpublishCourse(1L);

        assertEquals(ContentStatus.PRIVATE, publishedCourse.getStatus());
    }

    @Test
    @DisplayName("어드민이 강좌를 비공개하면 상태가 PRIVATE이 된다")
    void givenAdminRole_whenUnpublishCourse_thenStatusIsPrivate() {
        authenticate(MemberRole.ADMIN);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(publishedCourse));

        courseService.unpublishCourse(1L);

        assertEquals(ContentStatus.PRIVATE, publishedCourse.getStatus());
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

    // --- addLecture ---

    @Test
    @DisplayName("강사가 강의를 추가하면 강의가 추가된다")
    void givenInstructorRole_whenAddLecture_thenLectureIsAdded() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.addLecture(1L, "새 강의", "/lectures/2");

        assertEquals(2, privateCourse.getLectures().size());
    }

    @Test
    @DisplayName("일반 회원이 강의를 추가하면 예외가 발생한다")
    void givenMemberRole_whenAddLecture_thenThrowsException() {
        authenticate(MemberRole.MEMBER);

        assertThrows(CourseException.class, () -> courseService.addLecture(1L, "새 강의", "/lectures/2"));
    }

    @Test
    @DisplayName("어드민이 강의를 추가하면 예외가 발생한다")
    void givenAdminRole_whenAddLecture_thenThrowsException() {
        authenticate(MemberRole.ADMIN);

        assertThrows(CourseException.class, () -> courseService.addLecture(1L, "새 강의", "/lectures/2"));
    }

    @Test
    @DisplayName("존재하지 않는 강좌에 강의를 추가하면 예외가 발생한다")
    void givenNonExistentCourse_whenAddLecture_thenThrowsException() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.addLecture(999L, "새 강의", "/lectures/2"));
    }

    // --- removeLecture ---

    @Test
    @DisplayName("강사가 강의를 삭제하면 강의가 제거된다")
    void givenInstructorRole_whenRemoveLecture_thenLectureIsRemoved() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.removeLecture(1L, 10L);

        assertEquals(0, privateCourse.getLectures().size());
    }

    @Test
    @DisplayName("어드민이 강의를 삭제하면 강의가 제거된다")
    void givenAdminRole_whenRemoveLecture_thenLectureIsRemoved() {
        authenticate(MemberRole.ADMIN);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.removeLecture(1L, 10L);

        assertEquals(0, privateCourse.getLectures().size());
    }

    @Test
    @DisplayName("일반 회원이 강의를 삭제하면 예외가 발생한다")
    void givenMemberRole_whenRemoveLecture_thenThrowsException() {
        authenticate(MemberRole.MEMBER);

        assertThrows(CourseException.class, () -> courseService.removeLecture(1L, 10L));
    }

    @Test
    @DisplayName("존재하지 않는 강좌에서 강의를 삭제하면 예외가 발생한다")
    void givenNonExistentCourse_whenRemoveLecture_thenThrowsException() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.removeLecture(999L, 10L));
    }

    @Test
    @DisplayName("존재하지 않는 강의를 삭제하면 예외가 발생한다")
    void givenNonExistentLecture_whenRemoveLecture_thenThrowsException() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseException.class, () -> courseService.removeLecture(1L, 999L));
    }

    // --- publishLecture ---

    @Test
    @DisplayName("강사가 강의를 공개하면 상태가 PUBLIC이 된다")
    void givenInstructorRole_whenPublishLecture_thenStatusIsPublic() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.publishLecture(1L, 10L);

        assertEquals(ContentStatus.PUBLIC, privateCourse.getLectures().get(0).getStatus());
    }

    @Test
    @DisplayName("어드민이 강의를 공개하면 예외가 발생한다")
    void givenAdminRole_whenPublishLecture_thenThrowsException() {
        authenticate(MemberRole.ADMIN);

        assertThrows(CourseException.class, () -> courseService.publishLecture(1L, 10L));
    }

    @Test
    @DisplayName("일반 회원이 강의를 공개하면 예외가 발생한다")
    void givenMemberRole_whenPublishLecture_thenThrowsException() {
        authenticate(MemberRole.MEMBER);

        assertThrows(CourseException.class, () -> courseService.publishLecture(1L, 10L));
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 강의를 공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenPublishLecture_thenThrowsException() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.publishLecture(999L, 10L));
    }

    // --- unpublishLecture ---

    @Test
    @DisplayName("강사가 강의를 비공개하면 상태가 PRIVATE이 된다")
    void givenInstructorRole_whenUnpublishLecture_thenStatusIsPrivate() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        privateCourse.publishLecture(new LectureId(10L));

        courseService.unpublishLecture(1L, 10L);

        assertEquals(ContentStatus.PRIVATE, privateCourse.getLectures().get(0).getStatus());
    }

    @Test
    @DisplayName("어드민이 강의를 비공개하면 상태가 PRIVATE이 된다")
    void givenAdminRole_whenUnpublishLecture_thenStatusIsPrivate() {
        authenticate(MemberRole.ADMIN);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        privateCourse.publishLecture(new LectureId(10L));

        courseService.unpublishLecture(1L, 10L);

        assertEquals(ContentStatus.PRIVATE, privateCourse.getLectures().get(0).getStatus());
    }

    @Test
    @DisplayName("일반 회원이 강의를 비공개하면 예외가 발생한다")
    void givenMemberRole_whenUnpublishLecture_thenThrowsException() {
        authenticate(MemberRole.MEMBER);

        assertThrows(CourseException.class, () -> courseService.unpublishLecture(1L, 10L));
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 강의를 비공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenUnpublishLecture_thenThrowsException() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.unpublishLecture(999L, 10L));
    }

    // --- addMission ---

    @Test
    @DisplayName("강사가 미션을 추가하면 미션이 추가된다")
    void givenInstructorRole_whenAddMission_thenMissionIsAdded() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.addMission(1L, "새 미션", "문제 내용");

        assertEquals(2, privateCourse.getMissions().size());
    }

    @Test
    @DisplayName("일반 회원이 미션을 추가하면 예외가 발생한다")
    void givenMemberRole_whenAddMission_thenThrowsException() {
        authenticate(MemberRole.MEMBER);

        assertThrows(CourseException.class, () -> courseService.addMission(1L, "새 미션", "문제 내용"));
    }

    @Test
    @DisplayName("어드민이 미션을 추가하면 예외가 발생한다")
    void givenAdminRole_whenAddMission_thenThrowsException() {
        authenticate(MemberRole.ADMIN);

        assertThrows(CourseException.class, () -> courseService.addMission(1L, "새 미션", "문제 내용"));
    }

    @Test
    @DisplayName("존재하지 않는 강좌에 미션을 추가하면 예외가 발생한다")
    void givenNonExistentCourse_whenAddMission_thenThrowsException() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.addMission(999L, "새 미션", "문제 내용"));
    }

    // --- removeMission ---

    @Test
    @DisplayName("강사가 미션을 삭제하면 미션이 제거된다")
    void givenInstructorRole_whenRemoveMission_thenMissionIsRemoved() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.removeMission(1L, 20L);

        assertEquals(0, privateCourse.getMissions().size());
    }

    @Test
    @DisplayName("어드민이 미션을 삭제하면 미션이 제거된다")
    void givenAdminRole_whenRemoveMission_thenMissionIsRemoved() {
        authenticate(MemberRole.ADMIN);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.removeMission(1L, 20L);

        assertEquals(0, privateCourse.getMissions().size());
    }

    @Test
    @DisplayName("일반 회원이 미션을 삭제하면 예외가 발생한다")
    void givenMemberRole_whenRemoveMission_thenThrowsException() {
        authenticate(MemberRole.MEMBER);

        assertThrows(CourseException.class, () -> courseService.removeMission(1L, 20L));
    }

    @Test
    @DisplayName("존재하지 않는 강좌에서 미션을 삭제하면 예외가 발생한다")
    void givenNonExistentCourse_whenRemoveMission_thenThrowsException() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.removeMission(999L, 20L));
    }

    @Test
    @DisplayName("존재하지 않는 미션을 삭제하면 예외가 발생한다")
    void givenNonExistentMission_whenRemoveMission_thenThrowsException() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseException.class, () -> courseService.removeMission(1L, 999L));
    }

    // --- publishMission ---

    @Test
    @DisplayName("강사가 미션을 공개하면 상태가 PUBLIC이 된다")
    void givenInstructorRole_whenPublishMission_thenStatusIsPublic() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.publishMission(1L, 20L);

        assertEquals(ContentStatus.PUBLIC, privateCourse.getMissions().get(0).getStatus());
    }

    @Test
    @DisplayName("어드민이 미션을 공개하면 예외가 발생한다")
    void givenAdminRole_whenPublishMission_thenThrowsException() {
        authenticate(MemberRole.ADMIN);

        assertThrows(CourseException.class, () -> courseService.publishMission(1L, 20L));
    }

    @Test
    @DisplayName("일반 회원이 미션을 공개하면 예외가 발생한다")
    void givenMemberRole_whenPublishMission_thenThrowsException() {
        authenticate(MemberRole.MEMBER);

        assertThrows(CourseException.class, () -> courseService.publishMission(1L, 20L));
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 미션을 공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenPublishMission_thenThrowsException() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.publishMission(999L, 20L));
    }

    // --- unpublishMission ---

    @Test
    @DisplayName("강사가 미션을 비공개하면 상태가 PRIVATE이 된다")
    void givenInstructorRole_whenUnpublishMission_thenStatusIsPrivate() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        privateCourse.publishMission(new MissionId(20L));

        courseService.unpublishMission(1L, 20L);

        assertEquals(ContentStatus.PRIVATE, privateCourse.getMissions().get(0).getStatus());
    }

    @Test
    @DisplayName("어드민이 미션을 비공개하면 상태가 PRIVATE이 된다")
    void givenAdminRole_whenUnpublishMission_thenStatusIsPrivate() {
        authenticate(MemberRole.ADMIN);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        privateCourse.publishMission(new MissionId(20L));

        courseService.unpublishMission(1L, 20L);

        assertEquals(ContentStatus.PRIVATE, privateCourse.getMissions().get(0).getStatus());
    }

    @Test
    @DisplayName("일반 회원이 미션을 비공개하면 예외가 발생한다")
    void givenMemberRole_whenUnpublishMission_thenThrowsException() {
        authenticate(MemberRole.MEMBER);

        assertThrows(CourseException.class, () -> courseService.unpublishMission(1L, 20L));
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 미션을 비공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenUnpublishMission_thenThrowsException() {
        authenticate(MemberRole.INSTRUCTOR);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.unpublishMission(999L, 20L));
    }
}
