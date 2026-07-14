package com.lcs.lxp.course.service;

import com.lcs.lxp.course.dto.response.CourseDetailResponse;
import com.lcs.lxp.course.dto.response.CoursePageResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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

    // --- getCourses ---

    @Test
    @DisplayName("키워드 없이 강좌 목록 조회 시 공개된 강좌 페이지를 반환한다")
    void givenNoKeyword_whenGetCourses_thenReturnsPublicCoursePage() {
        ReflectionTestUtils.setField(publishedCourse, "id", 1L);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Course> page = new PageImpl<>(List.of(publishedCourse), pageable, 1);
        when(courseRepository.findAllByStatus(ContentStatus.PUBLIC, pageable)).thenReturn(page);

        CoursePageResponse result = courseService.getCourses(null, 0, 10);

        assertEquals(1L, result.totalElements());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1, result.courses().size());
        verify(courseRepository).findAllByStatus(ContentStatus.PUBLIC, pageable);
    }

    @Test
    @DisplayName("빈 키워드로 강좌 목록 조회 시 공개된 강좌 페이지를 반환한다")
    void givenBlankKeyword_whenGetCourses_thenReturnsPublicCoursePage() {
        ReflectionTestUtils.setField(publishedCourse, "id", 1L);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Course> page = new PageImpl<>(List.of(publishedCourse), pageable, 1);
        when(courseRepository.findAllByStatus(ContentStatus.PUBLIC, pageable)).thenReturn(page);

        CoursePageResponse result = courseService.getCourses("   ", 0, 10);

        assertEquals(1L, result.totalElements());
        verify(courseRepository).findAllByStatus(ContentStatus.PUBLIC, pageable);
    }

    @Test
    @DisplayName("키워드로 강좌 목록 검색 시 키워드가 포함된 공개 강좌 페이지를 반환한다")
    void givenKeyword_whenGetCourses_thenReturnsFilteredCoursePage() {
        ReflectionTestUtils.setField(publishedCourse, "id", 1L);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Course> page = new PageImpl<>(List.of(publishedCourse), pageable, 1);
        when(courseRepository.findByStatusAndTitleKeyword(ContentStatus.PUBLIC, "강좌", pageable)).thenReturn(page);

        CoursePageResponse result = courseService.getCourses("강좌", 0, 10);

        assertEquals(1L, result.totalElements());
        assertEquals(1, result.courses().size());
        verify(courseRepository).findByStatusAndTitleKeyword(ContentStatus.PUBLIC, "강좌", pageable);
    }

    @Test
    @DisplayName("키워드 검색 결과가 없으면 빈 페이지를 반환한다")
    void givenKeywordWithNoMatch_whenGetCourses_thenReturnsEmptyPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Course> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(courseRepository.findByStatusAndTitleKeyword(ContentStatus.PUBLIC, "없는키워드", pageable)).thenReturn(emptyPage);

        CoursePageResponse result = courseService.getCourses("없는키워드", 0, 10);

        assertEquals(0L, result.totalElements());
        assertEquals(0, result.courses().size());
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
    @DisplayName("강좌 생성을 요청하면 저장된다")
    void givenValidRequest_whenCreateCourse_thenCourseIsSaved() {
        courseService.createCourse(1L, "강좌 제목", "강좌 설명", null);

        verify(courseRepository).save(any(Course.class));
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
    @DisplayName("강좌를 공개하면 상태가 PUBLIC이 된다")
    void givenPrivateCourse_whenPublishCourse_thenStatusIsPublic() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.publishCourse(1L);

        assertEquals(ContentStatus.PUBLIC, privateCourse.getStatus());
    }

    @Test
    @DisplayName("존재하지 않는 강좌를 공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenPublishCourse_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.publishCourse(999L));
    }

    // --- unpublishCourse ---

    @Test
    @DisplayName("강좌를 비공개하면 상태가 PRIVATE이 된다")
    void givenPublishedCourse_whenUnpublishCourse_thenStatusIsPrivate() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(publishedCourse));

        courseService.unpublishCourse(1L);

        assertEquals(ContentStatus.PRIVATE, publishedCourse.getStatus());
    }

    @Test
    @DisplayName("존재하지 않는 강좌를 비공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenUnpublishCourse_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.unpublishCourse(999L));
    }

    // --- addLecture ---

    @Test
    @DisplayName("강의를 추가하면 강의가 추가된다")
    void givenValidRequest_whenAddLecture_thenLectureIsAdded() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.addLecture(1L, "새 강의", "/lectures/2");

        assertEquals(2, privateCourse.getLectures().size());
    }

    @Test
    @DisplayName("존재하지 않는 강좌에 강의를 추가하면 예외가 발생한다")
    void givenNonExistentCourse_whenAddLecture_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.addLecture(999L, "새 강의", "/lectures/2"));
    }

    // --- publishLecture ---

    @Test
    @DisplayName("강의를 공개하면 상태가 PUBLIC이 된다")
    void givenPrivateLecture_whenPublishLecture_thenStatusIsPublic() {
        privateCourse.unpublishLecture(new LectureId(10L));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.publishLecture(1L, 10L);

        assertEquals(ContentStatus.PUBLIC, privateCourse.getLectures().get(0).getStatus());
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 강의를 공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenPublishLecture_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.publishLecture(999L, 10L));
    }

    // --- unpublishLecture ---

    @Test
    @DisplayName("강의를 비공개하면 상태가 PRIVATE이 된다")
    void givenPublishedLecture_whenUnpublishLecture_thenStatusIsPrivate() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        privateCourse.publishLecture(new LectureId(10L));

        courseService.unpublishLecture(1L, 10L);

        assertEquals(ContentStatus.PRIVATE, privateCourse.getLectures().get(0).getStatus());
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 강의를 비공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenUnpublishLecture_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.unpublishLecture(999L, 10L));
    }

    // --- addMission ---

    @Test
    @DisplayName("미션을 추가하면 미션이 추가된다")
    void givenValidRequest_whenAddMission_thenMissionIsAdded() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.addMission(1L, "새 미션", "문제 내용");

        assertEquals(2, privateCourse.getMissions().size());
    }

    @Test
    @DisplayName("존재하지 않는 강좌에 미션을 추가하면 예외가 발생한다")
    void givenNonExistentCourse_whenAddMission_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.addMission(999L, "새 미션", "문제 내용"));
    }

    // --- publishMission ---

    @Test
    @DisplayName("미션을 공개하면 상태가 PUBLIC이 된다")
    void givenPrivateMission_whenPublishMission_thenStatusIsPublic() {
        privateCourse.unpublishMission(new MissionId(20L));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.publishMission(1L, 20L);

        assertEquals(ContentStatus.PUBLIC, privateCourse.getMissions().get(0).getStatus());
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 미션을 공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenPublishMission_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.publishMission(999L, 20L));
    }

    // --- unpublishMission ---

    @Test
    @DisplayName("미션을 비공개하면 상태가 PRIVATE이 된다")
    void givenPublishedMission_whenUnpublishMission_thenStatusIsPrivate() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        privateCourse.publishMission(new MissionId(20L));

        courseService.unpublishMission(1L, 20L);

        assertEquals(ContentStatus.PRIVATE, privateCourse.getMissions().get(0).getStatus());
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 미션을 비공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenUnpublishMission_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.unpublishMission(999L, 20L));
    }
}
