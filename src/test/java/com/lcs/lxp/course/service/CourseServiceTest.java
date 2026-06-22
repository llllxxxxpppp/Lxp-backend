package com.lcs.lxp.course.service;

import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.model.ContentStatus;
import com.lcs.lxp.course.model.Course;
import com.lcs.lxp.course.model.InstructorId;
import com.lcs.lxp.course.model.Title;
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
        privateCourse.addMission(new Title("미션"));

        publishedCourse = Course.create(new InstructorId(1L), new Title("강좌 제목"));
        publishedCourse.addLecture(new Title("강의"));
        publishedCourse.addMission(new Title("미션"));
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
