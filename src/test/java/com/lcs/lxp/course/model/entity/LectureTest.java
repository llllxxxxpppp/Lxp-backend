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

class LectureTest {

    private Course privateCourse;
    private Course publicCourse;

    @BeforeEach
    void setUp() {
        privateCourse = Course.create(new InstructorId(1L), new Title("강좌"), "강좌 설명", null);
        publicCourse = Course.create(new InstructorId(1L), new Title("강좌"), "강좌 설명", null);
        publicCourse.addLecture(new Title("강의"));
        publicCourse.addMission(new Title("미션"), "문제 내용");
        publicCourse.publish();
    }

    @Test
    @DisplayName("강의는 비공개 상태로 생성된다")
    void givenPrivateCourse_whenAddLecture_thenStatusIsPrivate() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"));
        assertEquals(ContentStatus.PRIVATE, lecture.getStatus());
    }

    @Test
    @DisplayName("강의 생성 시 삭제 상태가 아니다")
    void givenPrivateCourse_whenAddLecture_thenNotDeleted() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"));
        assertFalse(lecture.isDeleted());
    }

    @Test
    @DisplayName("강좌가 비공개이면 강의가 공개 상태여도 수정할 수 있다")
    void givenPrivateCourseAndPublicLecture_whenUpdateLecture_thenSucceeds() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"));
        lecture.publish();
        assertDoesNotThrow(() -> lecture.update(new Title("수정된 강의"), "/lectures/1"));
    }

    @Test
    @DisplayName("강좌가 비공개이면 강의가 비공개 상태여도 수정할 수 있다")
    void givenPrivateCourseAndPrivateLecture_whenUpdateLecture_thenSucceeds() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"));
        assertDoesNotThrow(() -> lecture.update(new Title("수정된 강의"), "/lectures/1"));
    }

    @Test
    @DisplayName("강좌가 공개이고 강의가 비공개이면 수정할 수 있다")
    void givenPublicCourseAndPrivateLecture_whenUpdateLecture_thenSucceeds() {
        Lecture lecture = publicCourse.getLectures().get(0);
        assertDoesNotThrow(() -> lecture.update(new Title("수정된 강의"), "/lectures/1"));
    }

    @Test
    @DisplayName("강좌가 공개이고 강의도 공개이면 수정할 수 없다")
    void givenPublicCourseAndPublicLecture_whenUpdateLecture_thenThrowsException() {
        Lecture lecture = publicCourse.getLectures().get(0);
        lecture.publish();
        assertThrows(CourseException.class, () -> lecture.update(new Title("수정된 강의"), "/lectures/1"));
    }

    @Test
    @DisplayName("강의 자료 URL이 null이면 수정할 수 없다")
    void givenNullContentUrl_whenUpdateLecture_thenThrowsException() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"));
        assertThrows(CourseException.class, () -> lecture.update(new Title("수정된 강의"), null));
    }

    @Test
    @DisplayName("강의 자료 URL이 빈 문자열이면 수정할 수 없다")
    void givenBlankContentUrl_whenUpdateLecture_thenThrowsException() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"));
        assertThrows(CourseException.class, () -> lecture.update(new Title("수정된 강의"), "  "));
    }

    @Test
    @DisplayName("강의를 공개할 수 있다")
    void givenPrivateLecture_whenPublish_thenStatusIsPublic() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"));
        lecture.publish();
        assertEquals(ContentStatus.PUBLIC, lecture.getStatus());
    }

    @Test
    @DisplayName("강의를 비공개하면 삭제 상태가 된다")
    void givenPublicLecture_whenUnpublish_thenStatusIsPrivateAndDeleted() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"));
        lecture.publish();
        lecture.unpublish();

        assertEquals(ContentStatus.PRIVATE, lecture.getStatus());
        assertTrue(lecture.isDeleted());
    }
}
