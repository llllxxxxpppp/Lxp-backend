package com.lcs.lxp.course.model.entity;

import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.model.vo.ContentStatus;
import com.lcs.lxp.course.model.vo.InstructorId;
import com.lcs.lxp.course.model.vo.Title;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseTest {

    private InstructorId instructorId;
    private Title title;

    @BeforeEach
    void setUp() {
        instructorId = new InstructorId(1L);
        title = new Title("강좌 제목");
    }

    @Test
    @DisplayName("강좌는 비공개 상태로 생성된다")
    void givenInstructor_whenCreateCourse_thenStatusIsPrivate() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        assertEquals(ContentStatus.PRIVATE, course.getStatus());
    }

    @Test
    @DisplayName("강좌 생성 시 삭제 상태가 아니다")
    void givenInstructor_whenCreateCourse_thenNotDeleted() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        assertFalse(course.isDeleted());
    }

    @Test
    @DisplayName("설명이 null이면 강좌 생성 시 예외가 발생한다")
    void givenNullDescription_whenCreateCourse_thenThrowsException() {
        assertThrows(CourseException.class, () -> Course.create(instructorId, title, null, null));
    }

    @Test
    @DisplayName("설명이 빈 문자열이면 강좌 생성 시 예외가 발생한다")
    void givenBlankDescription_whenCreateCourse_thenThrowsException() {
        assertThrows(CourseException.class, () -> Course.create(instructorId, title, "   ", null));
    }

    @Test
    @DisplayName("설명이 4096자를 초과하면 강좌 생성 시 예외가 발생한다")
    void givenDescriptionExceedingMaxLength_whenCreateCourse_thenThrowsException() {
        String longDescription = "a".repeat(4097);
        assertThrows(CourseException.class, () -> Course.create(instructorId, title, longDescription, null));
    }

    @Test
    @DisplayName("비공개 상태에서 강좌 제목을 수정할 수 있다")
    void givenPrivateCourse_whenUpdate_thenSucceeds() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        assertDoesNotThrow(() -> course.update(new Title("수정된 제목"), "수정된 설명", null));
    }

    @Test
    @DisplayName("공개 상태에서 강좌 제목을 수정하면 예외가 발생한다")
    void givenPublicCourse_whenUpdate_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"));
        course.addMission(new Title("미션"), "문제 내용");
        course.publish();

        assertThrows(CourseException.class, () -> course.update(new Title("수정된 제목"), "수정된 설명", null));
    }

    @Test
    @DisplayName("수정 시 설명이 null이면 예외가 발생한다")
    void givenNullDescriptionOnUpdate_whenUpdate_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        assertThrows(CourseException.class, () -> course.update(new Title("수정된 제목"), null, null));
    }

    @Test
    @DisplayName("수정 시 설명이 4096자를 초과하면 예외가 발생한다")
    void givenDescriptionExceedingMaxLengthOnUpdate_whenUpdate_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        String longDescription = "a".repeat(4097);
        assertThrows(CourseException.class, () -> course.update(new Title("수정된 제목"), longDescription, null));
    }

    @Test
    @DisplayName("강의와 미션이 모두 없으면 공개할 수 없다")
    void givenCourseWithNoLectureAndMission_whenPublish_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        assertThrows(CourseException.class, course::publish);
    }

    @Test
    @DisplayName("강의만 있고 미션이 없으면 공개할 수 없다")
    void givenCourseWithLectureOnly_whenPublish_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"));
        assertThrows(CourseException.class, course::publish);
    }

    @Test
    @DisplayName("미션만 있고 강의가 없으면 공개할 수 없다")
    void givenCourseWithMissionOnly_whenPublish_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addMission(new Title("미션"), "문제 내용");
        assertThrows(CourseException.class, course::publish);
    }

    @Test
    @DisplayName("강의와 미션이 각각 1개 이상이면 공개할 수 있다")
    void givenCourseWithLectureAndMission_whenPublish_thenStatusIsPublic() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"));
        course.addMission(new Title("미션"), "문제 내용");
        assertDoesNotThrow(course::publish);
        assertEquals(ContentStatus.PUBLIC, course.getStatus());
    }

    @Test
    @DisplayName("비공개 처리하면 삭제 상태가 된다")
    void givenPublicCourse_whenUnpublish_thenStatusIsPrivateAndDeleted() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"));
        course.addMission(new Title("미션"), "문제 내용");
        course.publish();
        course.unpublish();

        assertEquals(ContentStatus.PRIVATE, course.getStatus());
        assertTrue(course.isDeleted());
    }

    @Test
    @DisplayName("공개 상태에서 강의를 추가하면 예외가 발생한다")
    void givenPublicCourse_whenAddLecture_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"));
        course.addMission(new Title("미션"), "문제 내용");
        course.publish();

        assertThrows(CourseException.class, () -> course.addLecture(new Title("강의2")));
    }

    @Test
    @DisplayName("공개 상태에서 미션을 추가하면 예외가 발생한다")
    void givenPublicCourse_whenAddMission_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"));
        course.addMission(new Title("미션"), "문제 내용");
        course.publish();

        assertThrows(CourseException.class, () -> course.addMission(new Title("미션2"), "문제 내용"));
    }

    @Test
    @DisplayName("비공개된 강의만 있으면 공개할 수 없다")
    void givenCourseWithOnlyDeletedLecture_whenPublish_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"));
        course.addMission(new Title("미션"), "문제 내용");
        lecture.unpublish();

        assertThrows(CourseException.class, course::publish);
    }

    @Test
    @DisplayName("비공개된 미션만 있으면 공개할 수 없다")
    void givenCourseWithOnlyDeletedMission_whenPublish_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"));
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        mission.unpublish();

        assertThrows(CourseException.class, course::publish);
    }
}
