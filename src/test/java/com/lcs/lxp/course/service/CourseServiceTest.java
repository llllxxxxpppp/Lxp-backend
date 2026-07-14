package com.lcs.lxp.course.service;

import com.lcs.lxp.course.dto.response.CourseDetailResponse;
import com.lcs.lxp.course.dto.response.CourseItemResponse;
import com.lcs.lxp.course.dto.response.CoursePageResponse;
import com.lcs.lxp.course.dto.response.CourseSummaryResponse;
import com.lcs.lxp.course.exception.CourseAccessDeniedException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    /**
     * COURSE-09: 소유권 검증 테스트에서 사용하는 요청자 ID 상수.
     * privateCourse/publishedCourse는 모두 {@code OWNER_ID}(1L)가 작성한 강좌로 생성된다.
     */
    private static final long OWNER_ID = 1L;
    private static final long OTHER_INSTRUCTOR_ID = 2L;
    private static final long ADMIN_REQUESTER_ID = 999L;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    private Course privateCourse;
    private Course publishedCourse;

    @BeforeEach
    void setUp() {
        privateCourse = Course.create(new InstructorId(1L), new Title("강좌 제목"), "강좌 설명", null);
        Lecture privateLecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(privateLecture, "id", 10L);
        Mission privateMission = privateCourse.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(privateMission, "id", 20L);

        publishedCourse = Course.create(new InstructorId(1L), new Title("강좌 제목"), "강좌 설명", null);
        Lecture publishedLecture = publishedCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
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
        ReflectionTestUtils.setField(course.addLecture(new Title("강의 제목"), "/lectures/1", "mp4"), "id", 10L);
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
        assertEquals("mp4", detail.lectures().get(0).contentType());
        assertEquals(1, detail.lectures().get(0).sortOrder());
        assertEquals(1, detail.missions().size());
        assertEquals(20L, detail.missions().get(0).missionId());
        assertEquals("미션 제목", detail.missions().get(0).title());
        assertEquals(2, detail.missions().get(0).sortOrder());

        List<CourseItemResponse> items = detail.items();
        assertEquals(2, items.size());
        assertEquals("LECTURE", items.get(0).type());
        assertEquals(10L, items.get(0).id());
        assertEquals("강의 제목", items.get(0).title());
        assertEquals(1, items.get(0).sortOrder());
        assertEquals("MISSION", items.get(1).type());
        assertEquals(20L, items.get(1).id());
        assertEquals("미션 제목", items.get(1).title());
        assertEquals(2, items.get(1).sortOrder());
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
    @DisplayName("비공개 강좌의 제목을 소유 강사가 수정하면 제목이 변경된다")
    void givenPrivateCourseOwnedByRequester_whenUpdateCourse_thenTitleIsUpdated() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.updateCourse(1L, "수정된 제목", "수정된 설명", null, OWNER_ID, false);

        assertEquals("수정된 제목", privateCourse.getTitle().getValue());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("어드민이 강좌를 수정하면 소유자가 아니어도 제목이 변경된다")
    void givenAdminRequester_whenUpdateCourse_thenTitleIsUpdated() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.updateCourse(1L, "수정된 제목", "수정된 설명", null, ADMIN_REQUESTER_ID, true);

        assertEquals("수정된 제목", privateCourse.getTitle().getValue());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 수정을 시도하면 접근 거부 예외가 발생한다")
    void givenOtherInstructorRequester_whenUpdateCourse_thenThrowsAccessDeniedException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseAccessDeniedException.class,
                () -> courseService.updateCourse(1L, "수정된 제목", "수정된 설명", null, OTHER_INSTRUCTOR_ID, false));

        assertEquals("강좌 제목", privateCourse.getTitle().getValue());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("공개 강좌의 제목을 소유 강사가 수정하면 예외가 발생한다")
    void givenPublicCourse_whenUpdateCourse_thenThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(publishedCourse));

        assertThrows(CourseException.class,
                () -> courseService.updateCourse(1L, "수정된 제목", "수정된 설명", null, OWNER_ID, false));
    }

    @Test
    @DisplayName("존재하지 않는 강좌를 수정하면 예외가 발생한다")
    void givenNonExistentCourse_whenUpdateCourse_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class,
                () -> courseService.updateCourse(999L, "수정된 제목", "수정된 설명", null, OWNER_ID, false));
    }

    // --- publishCourse ---

    @Test
    @DisplayName("소유 강사가 강좌를 공개하면 상태가 PUBLIC이 된다")
    void givenPrivateCourseOwnedByRequester_whenPublishCourse_thenStatusIsPublic() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.publishCourse(1L, OWNER_ID, false);

        assertEquals(ContentStatus.PUBLIC, privateCourse.getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("어드민이 강좌를 공개하면 소유자가 아니어도 상태가 PUBLIC이 된다")
    void givenAdminRequester_whenPublishCourse_thenStatusIsPublic() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.publishCourse(1L, ADMIN_REQUESTER_ID, true);

        assertEquals(ContentStatus.PUBLIC, privateCourse.getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 공개를 시도하면 접근 거부 예외가 발생한다")
    void givenOtherInstructorRequester_whenPublishCourse_thenThrowsAccessDeniedException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseAccessDeniedException.class,
                () -> courseService.publishCourse(1L, OTHER_INSTRUCTOR_ID, false));

        assertEquals(ContentStatus.PRIVATE, privateCourse.getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 강좌를 공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenPublishCourse_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.publishCourse(999L, OWNER_ID, false));
    }

    // --- unpublishCourse ---

    @Test
    @DisplayName("소유 강사가 강좌를 비공개하면 상태가 PRIVATE이 된다")
    void givenPublishedCourseOwnedByRequester_whenUnpublishCourse_thenStatusIsPrivate() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(publishedCourse));

        courseService.unpublishCourse(1L, OWNER_ID, false);

        assertEquals(ContentStatus.PRIVATE, publishedCourse.getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("어드민이 강좌를 비공개하면 소유자가 아니어도 상태가 PRIVATE이 된다")
    void givenAdminRequester_whenUnpublishCourse_thenStatusIsPrivate() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(publishedCourse));

        courseService.unpublishCourse(1L, ADMIN_REQUESTER_ID, true);

        assertEquals(ContentStatus.PRIVATE, publishedCourse.getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 비공개를 시도하면 접근 거부 예외가 발생한다")
    void givenOtherInstructorRequester_whenUnpublishCourse_thenThrowsAccessDeniedException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(publishedCourse));

        assertThrows(CourseAccessDeniedException.class,
                () -> courseService.unpublishCourse(1L, OTHER_INSTRUCTOR_ID, false));

        assertEquals(ContentStatus.PUBLIC, publishedCourse.getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 강좌를 비공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenUnpublishCourse_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.unpublishCourse(999L, OWNER_ID, false));
    }

    // --- addLecture ---

    @Test
    @DisplayName("강의를 추가하면 강의가 추가된다")
    void givenValidRequest_whenAddLecture_thenLectureIsAdded() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.addLecture(1L, "새 강의", "/lectures/2", "mp4");

        assertEquals(2, privateCourse.getLectures().size());
        assertEquals("mp4", privateCourse.getLectures().get(1).getContentType());
    }

    @Test
    @DisplayName("존재하지 않는 강좌에 강의를 추가하면 예외가 발생한다")
    void givenNonExistentCourse_whenAddLecture_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.addLecture(999L, "새 강의", "/lectures/2", "mp4"));
    }

    @Test
    @DisplayName("자료 타입이 null이면 강의를 추가할 수 없다")
    void givenNullContentType_whenAddLecture_thenThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseException.class, () -> courseService.addLecture(1L, "새 강의", "/lectures/2", null));
    }

    @Test
    @DisplayName("자료 타입이 빈 문자열이면 강의를 추가할 수 없다")
    void givenBlankContentType_whenAddLecture_thenThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseException.class, () -> courseService.addLecture(1L, "새 강의", "/lectures/2", "   "));
    }

    // --- publishLecture ---

    @Test
    @DisplayName("소유 강사가 강의를 공개하면 상태가 PUBLIC이 된다")
    void givenPrivateLectureOwnedByRequester_whenPublishLecture_thenStatusIsPublic() {
        privateCourse.unpublishLecture(new LectureId(10L));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.publishLecture(1L, 10L, OWNER_ID, false);

        assertEquals(ContentStatus.PUBLIC, privateCourse.getLectures().get(0).getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("어드민이 강의를 공개하면 소유자가 아니어도 상태가 PUBLIC이 된다")
    void givenAdminRequester_whenPublishLecture_thenStatusIsPublic() {
        privateCourse.unpublishLecture(new LectureId(10L));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.publishLecture(1L, 10L, ADMIN_REQUESTER_ID, true);

        assertEquals(ContentStatus.PUBLIC, privateCourse.getLectures().get(0).getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 강의 공개를 시도하면 접근 거부 예외가 발생한다")
    void givenOtherInstructorRequester_whenPublishLecture_thenThrowsAccessDeniedException() {
        privateCourse.unpublishLecture(new LectureId(10L));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseAccessDeniedException.class,
                () -> courseService.publishLecture(1L, 10L, OTHER_INSTRUCTOR_ID, false));

        assertEquals(ContentStatus.PRIVATE, privateCourse.getLectures().get(0).getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 강의를 공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenPublishLecture_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.publishLecture(999L, 10L, OWNER_ID, false));
    }

    // --- unpublishLecture ---

    @Test
    @DisplayName("소유 강사가 강의를 비공개하면 상태가 PRIVATE이 된다")
    void givenPublishedLectureOwnedByRequester_whenUnpublishLecture_thenStatusIsPrivate() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        privateCourse.publishLecture(new LectureId(10L));

        courseService.unpublishLecture(1L, 10L, OWNER_ID, false);

        assertEquals(ContentStatus.PRIVATE, privateCourse.getLectures().get(0).getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("어드민이 강의를 비공개하면 소유자가 아니어도 상태가 PRIVATE이 된다")
    void givenAdminRequester_whenUnpublishLecture_thenStatusIsPrivate() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        privateCourse.publishLecture(new LectureId(10L));

        courseService.unpublishLecture(1L, 10L, ADMIN_REQUESTER_ID, true);

        assertEquals(ContentStatus.PRIVATE, privateCourse.getLectures().get(0).getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 강의 비공개를 시도하면 접근 거부 예외가 발생한다")
    void givenOtherInstructorRequester_whenUnpublishLecture_thenThrowsAccessDeniedException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        privateCourse.publishLecture(new LectureId(10L));

        assertThrows(CourseAccessDeniedException.class,
                () -> courseService.unpublishLecture(1L, 10L, OTHER_INSTRUCTOR_ID, false));

        assertEquals(ContentStatus.PUBLIC, privateCourse.getLectures().get(0).getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 강의를 비공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenUnpublishLecture_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.unpublishLecture(999L, 10L, OWNER_ID, false));
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
    @DisplayName("소유 강사가 미션을 공개하면 상태가 PUBLIC이 된다")
    void givenPrivateMissionOwnedByRequester_whenPublishMission_thenStatusIsPublic() {
        privateCourse.unpublishMission(new MissionId(20L));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.publishMission(1L, 20L, OWNER_ID, false);

        assertEquals(ContentStatus.PUBLIC, privateCourse.getMissions().get(0).getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("어드민이 미션을 공개하면 소유자가 아니어도 상태가 PUBLIC이 된다")
    void givenAdminRequester_whenPublishMission_thenStatusIsPublic() {
        privateCourse.unpublishMission(new MissionId(20L));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.publishMission(1L, 20L, ADMIN_REQUESTER_ID, true);

        assertEquals(ContentStatus.PUBLIC, privateCourse.getMissions().get(0).getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 미션 공개를 시도하면 접근 거부 예외가 발생한다")
    void givenOtherInstructorRequester_whenPublishMission_thenThrowsAccessDeniedException() {
        privateCourse.unpublishMission(new MissionId(20L));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseAccessDeniedException.class,
                () -> courseService.publishMission(1L, 20L, OTHER_INSTRUCTOR_ID, false));

        assertEquals(ContentStatus.PRIVATE, privateCourse.getMissions().get(0).getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 미션을 공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenPublishMission_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.publishMission(999L, 20L, OWNER_ID, false));
    }

    // --- unpublishMission ---

    @Test
    @DisplayName("소유 강사가 미션을 비공개하면 상태가 PRIVATE이 된다")
    void givenPublishedMissionOwnedByRequester_whenUnpublishMission_thenStatusIsPrivate() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        privateCourse.publishMission(new MissionId(20L));

        courseService.unpublishMission(1L, 20L, OWNER_ID, false);

        assertEquals(ContentStatus.PRIVATE, privateCourse.getMissions().get(0).getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("어드민이 미션을 비공개하면 소유자가 아니어도 상태가 PRIVATE이 된다")
    void givenAdminRequester_whenUnpublishMission_thenStatusIsPrivate() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        privateCourse.publishMission(new MissionId(20L));

        courseService.unpublishMission(1L, 20L, ADMIN_REQUESTER_ID, true);

        assertEquals(ContentStatus.PRIVATE, privateCourse.getMissions().get(0).getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 미션 비공개를 시도하면 접근 거부 예외가 발생한다")
    void givenOtherInstructorRequester_whenUnpublishMission_thenThrowsAccessDeniedException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        privateCourse.publishMission(new MissionId(20L));

        assertThrows(CourseAccessDeniedException.class,
                () -> courseService.unpublishMission(1L, 20L, OTHER_INSTRUCTOR_ID, false));

        assertEquals(ContentStatus.PUBLIC, privateCourse.getMissions().get(0).getStatus());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 미션을 비공개하면 예외가 발생한다")
    void givenNonExistentCourse_whenUnpublishMission_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.unpublishMission(999L, 20L, OWNER_ID, false));
    }

    // --- deleteCourse ---

    @Test
    @DisplayName("소유 강사가 강좌를 삭제하면 삭제 상태가 된다")
    void givenExistingCourseOwnedByRequester_whenDeleteCourse_thenCourseIsDeleted() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.deleteCourse(1L, OWNER_ID, false);

        assertTrue(privateCourse.isDeleted());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("어드민이 강좌를 삭제하면 소유자가 아니어도 삭제 상태가 된다")
    void givenAdminRequester_whenDeleteCourse_thenCourseIsDeleted() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.deleteCourse(1L, ADMIN_REQUESTER_ID, true);

        assertTrue(privateCourse.isDeleted());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 삭제를 시도하면 접근 거부 예외가 발생한다")
    void givenOtherInstructorRequester_whenDeleteCourse_thenThrowsAccessDeniedException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseAccessDeniedException.class,
                () -> courseService.deleteCourse(1L, OTHER_INSTRUCTOR_ID, false));

        assertFalse(privateCourse.isDeleted());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 강좌를 삭제하면 예외가 발생한다")
    void givenNonExistentCourse_whenDeleteCourse_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.deleteCourse(999L, OWNER_ID, false));
    }

    @Test
    @DisplayName("이미 삭제된 강좌를 다시 삭제하면 예외가 발생한다")
    void givenAlreadyDeletedCourse_whenDeleteCourse_thenThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        courseService.deleteCourse(1L, OWNER_ID, false);

        assertThrows(CourseException.class, () -> courseService.deleteCourse(1L, OWNER_ID, false));
    }

    // --- deleteLecture ---

    @Test
    @DisplayName("소유 강사가 강의를 삭제하면 삭제 상태가 된다")
    void givenExistingLectureOwnedByRequester_whenDeleteLecture_thenLectureIsDeleted() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.deleteLecture(1L, 10L, OWNER_ID, false);

        assertTrue(privateCourse.getLectures().get(0).isDeleted());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("어드민이 강의를 삭제하면 소유자가 아니어도 삭제 상태가 된다")
    void givenAdminRequester_whenDeleteLecture_thenLectureIsDeleted() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.deleteLecture(1L, 10L, ADMIN_REQUESTER_ID, true);

        assertTrue(privateCourse.getLectures().get(0).isDeleted());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 강의 삭제를 시도하면 접근 거부 예외가 발생한다")
    void givenOtherInstructorRequester_whenDeleteLecture_thenThrowsAccessDeniedException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseAccessDeniedException.class,
                () -> courseService.deleteLecture(1L, 10L, OTHER_INSTRUCTOR_ID, false));

        assertFalse(privateCourse.getLectures().get(0).isDeleted());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 강의를 삭제하면 예외가 발생한다")
    void givenNonExistentCourse_whenDeleteLecture_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.deleteLecture(999L, 10L, OWNER_ID, false));
    }

    @Test
    @DisplayName("이미 삭제된 강의를 다시 삭제하면 예외가 발생한다")
    void givenAlreadyDeletedLecture_whenDeleteLecture_thenThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        courseService.deleteLecture(1L, 10L, OWNER_ID, false);

        assertThrows(CourseException.class, () -> courseService.deleteLecture(1L, 10L, OWNER_ID, false));
    }

    // --- deleteMission ---

    @Test
    @DisplayName("소유 강사가 미션을 삭제하면 삭제 상태가 된다")
    void givenExistingMissionOwnedByRequester_whenDeleteMission_thenMissionIsDeleted() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.deleteMission(1L, 20L, OWNER_ID, false);

        assertTrue(privateCourse.getMissions().get(0).isDeleted());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("어드민이 미션을 삭제하면 소유자가 아니어도 삭제 상태가 된다")
    void givenAdminRequester_whenDeleteMission_thenMissionIsDeleted() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.deleteMission(1L, 20L, ADMIN_REQUESTER_ID, true);

        assertTrue(privateCourse.getMissions().get(0).isDeleted());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("강좌를 작성하지 않은 다른 강사가 미션 삭제를 시도하면 접근 거부 예외가 발생한다")
    void givenOtherInstructorRequester_whenDeleteMission_thenThrowsAccessDeniedException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseAccessDeniedException.class,
                () -> courseService.deleteMission(1L, 20L, OTHER_INSTRUCTOR_ID, false));

        assertFalse(privateCourse.getMissions().get(0).isDeleted());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 미션을 삭제하면 예외가 발생한다")
    void givenNonExistentCourse_whenDeleteMission_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class, () -> courseService.deleteMission(999L, 20L, OWNER_ID, false));
    }

    @Test
    @DisplayName("이미 삭제된 미션을 다시 삭제하면 예외가 발생한다")
    void givenAlreadyDeletedMission_whenDeleteMission_thenThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));
        courseService.deleteMission(1L, 20L, OWNER_ID, false);

        assertThrows(CourseException.class, () -> courseService.deleteMission(1L, 20L, OWNER_ID, false));
    }

    // --- reorderItems ---

    @Test
    @DisplayName("순서 변경 요청이 유효하면 강의/미션의 순번이 요청 순서대로 재배치된다")
    void givenValidRequest_whenReorderItems_thenSortOrdersAreReassigned() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        courseService.reorderItems(1L, List.of("MISSION", "LECTURE"), List.of(20L, 10L));

        assertEquals(1, privateCourse.getMissions().get(0).getSortOrder());
        assertEquals(2, privateCourse.getLectures().get(0).getSortOrder());
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 강좌의 순서를 변경하면 예외가 발생한다")
    void givenNonExistentCourse_whenReorderItems_thenThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseException.class,
                () -> courseService.reorderItems(999L, List.of("LECTURE"), List.of(10L)));
    }

    @Test
    @DisplayName("다른 강좌에 속한 강의/미션 ID가 섞여 있으면 순서 변경 시 예외가 발생한다")
    void givenItemNotBelongingToCourse_whenReorderItems_thenThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseException.class, () -> courseService.reorderItems(
                1L, List.of("LECTURE", "MISSION"), List.of(10L, 999L)));
    }

    @Test
    @DisplayName("강좌에 속한 강의/미션 일부만 순서 변경 대상으로 포함하면 예외가 발생한다")
    void givenPartialItemList_whenReorderItems_thenThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseException.class,
                () -> courseService.reorderItems(1L, List.of("LECTURE"), List.of(10L)));
    }

    @Test
    @DisplayName("순서 변경 대상 목록에 같은 항목이 중복되어 있으면 예외가 발생한다")
    void givenDuplicateItem_whenReorderItems_thenThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(privateCourse));

        assertThrows(CourseException.class, () -> courseService.reorderItems(
                1L, List.of("LECTURE", "LECTURE"), List.of(10L, 10L)));
    }

    // --- unpublishAllByInstructor (COURSE-08a: InstructorSuspendedEvent 처리) ---

    @Test
    @DisplayName("강사의 공개 강좌 목록을 대량 비공개 처리하면 모두 PRIVATE 상태가 된다")
    void givenInstructorWithMultiplePublicCourses_whenUnpublishAllByInstructor_thenAllCoursesBecomePrivate() {
        Course course1 = publishedCourseOf(1L, "강좌 1");
        Course course2 = publishedCourseOf(1L, "강좌 2");
        when(courseRepository.findAllByInstructorIdAndStatusAndDeletedAtIsNull(1L, ContentStatus.PUBLIC))
                .thenReturn(List.of(course1, course2));

        courseService.unpublishAllByInstructor(1L);

        assertEquals(ContentStatus.PRIVATE, course1.getStatus());
        assertEquals(ContentStatus.PRIVATE, course2.getStatus());
        verify(courseRepository).findAllByInstructorIdAndStatusAndDeletedAtIsNull(1L, ContentStatus.PUBLIC);
    }

    @Test
    @DisplayName("다른 강사의 강좌는 대량 비공개 처리 대상에서 제외되어 상태가 유지된다")
    void givenOtherInstructorPublicCourse_whenUnpublishAllByInstructor_thenOtherInstructorCourseIsUnaffected() {
        Course targetInstructorCourse = publishedCourseOf(1L, "강좌");
        Course otherInstructorCourse = publishedCourseOf(2L, "다른 강사 강좌");
        when(courseRepository.findAllByInstructorIdAndStatusAndDeletedAtIsNull(1L, ContentStatus.PUBLIC))
                .thenReturn(List.of(targetInstructorCourse));

        courseService.unpublishAllByInstructor(1L);

        assertEquals(ContentStatus.PRIVATE, targetInstructorCourse.getStatus());
        assertEquals(ContentStatus.PUBLIC, otherInstructorCourse.getStatus());
        verify(courseRepository).findAllByInstructorIdAndStatusAndDeletedAtIsNull(1L, ContentStatus.PUBLIC);
    }

    @Test
    @DisplayName("공개 강좌가 없는 강사를 대상으로 대량 비공개 처리를 요청하면 예외 없이 아무 강좌도 변경하지 않는다")
    void givenInstructorWithNoPublicCourses_whenUnpublishAllByInstructor_thenNoChangesOccur() {
        when(courseRepository.findAllByInstructorIdAndStatusAndDeletedAtIsNull(1L, ContentStatus.PUBLIC))
                .thenReturn(List.of());

        courseService.unpublishAllByInstructor(1L);

        verify(courseRepository).findAllByInstructorIdAndStatusAndDeletedAtIsNull(1L, ContentStatus.PUBLIC);
    }

    private Course publishedCourseOf(long instructorId, String title) {
        Course course = Course.create(new InstructorId(instructorId), new Title(title), "강좌 설명", null);
        course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        course.addMission(new Title("미션"), "문제 내용");
        course.publish();
        return course;
    }
}
