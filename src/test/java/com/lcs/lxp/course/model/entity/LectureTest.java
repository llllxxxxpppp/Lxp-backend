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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LectureTest {

    private Course privateCourse;
    private Course publicCourse;

    @BeforeEach
    void setUp() {
        privateCourse = Course.create(new InstructorId(1L), new Title("강좌"), "강좌 설명", null);
        publicCourse = Course.create(new InstructorId(1L), new Title("강좌"), "강좌 설명", null);
        publicCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        publicCourse.addMission(new Title("미션"), "문제 내용");
        publicCourse.publish();
    }

    @Test
    @DisplayName("강의는 공개 상태로 생성된다")
    void givenPrivateCourse_whenAddLecture_thenStatusIsPublic() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        assertEquals(ContentStatus.PUBLIC, lecture.getStatus());
    }

    @Test
    @DisplayName("강의 생성 시 생성일시는 not-null이고 수정일시는 null이다")
    void givenValidArguments_whenAddLecture_thenCreatedAtNotNullAndUpdatedAtNull() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");

        assertNotNull(lecture.getCreatedAt());
        assertNull(lecture.getUpdatedAt());
    }

    @Test
    @DisplayName("course가 null이면 강의를 생성할 수 없다")
    void givenNullCourse_whenCreateLecture_thenThrowsException() {
        assertThrows(CourseException.class, () -> Lecture.create(null, new Title("강의"), "/lectures/1", "mp4"));
    }

    @Test
    @DisplayName("title이 null이면 강의를 생성할 수 없다")
    void givenNullTitle_whenCreateLecture_thenThrowsException() {
        assertThrows(CourseException.class, () -> Lecture.create(privateCourse, null, "/lectures/1", "mp4"));
    }

    @Test
    @DisplayName("강의 자료 URL이 null이면 생성할 수 없다")
    void givenNullContentUrl_whenAddLecture_thenThrowsException() {
        assertThrows(CourseException.class, () -> privateCourse.addLecture(new Title("강의"), null, "mp4"));
    }

    @Test
    @DisplayName("강의 자료 URL이 빈 문자열이면 생성할 수 없다")
    void givenBlankContentUrl_whenAddLecture_thenThrowsException() {
        assertThrows(CourseException.class, () -> privateCourse.addLecture(new Title("강의"), "  ", "mp4"));
    }

    @Test
    @DisplayName("자료 타입이 null이면 강의를 생성할 수 없다")
    void givenNullContentType_whenAddLecture_thenThrowsException() {
        assertThrows(CourseException.class, () -> privateCourse.addLecture(new Title("강의"), "/lectures/1", null));
    }

    @Test
    @DisplayName("자료 타입이 빈 문자열이면 강의를 생성할 수 없다")
    void givenBlankContentType_whenAddLecture_thenThrowsException() {
        assertThrows(CourseException.class, () -> privateCourse.addLecture(new Title("강의"), "/lectures/1", "   "));
    }

    @Test
    @DisplayName("자료 타입을 지정하여 강의를 생성하면 자료 타입이 설정된다")
    void givenValidContentType_whenAddLecture_thenContentTypeIsSet() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        assertEquals("mp4", lecture.getContentType());
    }

    @Test
    @DisplayName("강좌가 비공개이면 강의가 공개 상태여도 수정할 수 있다")
    void givenPrivateCourseAndPublicLecture_whenUpdateLecture_thenSucceeds() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        lecture.publish();
        assertDoesNotThrow(() -> lecture.update(new Title("수정된 강의"), "/lectures/1", "mp4"));
    }

    @Test
    @DisplayName("강좌가 비공개이면 강의가 비공개 상태여도 수정할 수 있다")
    void givenPrivateCourseAndPrivateLecture_whenUpdateLecture_thenSucceeds() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        lecture.unpublish();
        assertDoesNotThrow(() -> lecture.update(new Title("수정된 강의"), "/lectures/1", "mp4"));
    }

    @Test
    @DisplayName("강좌가 공개이고 강의가 비공개이면 수정할 수 있다")
    void givenPublicCourseAndPrivateLecture_whenUpdateLecture_thenSucceeds() {
        Lecture lecture = publicCourse.getLectures().get(0);
        lecture.unpublish();
        assertDoesNotThrow(() -> lecture.update(new Title("수정된 강의"), "/lectures/1", "mp4"));
    }

    @Test
    @DisplayName("강좌가 공개이고 강의도 공개이면 수정할 수 없다")
    void givenPublicCourseAndPublicLecture_whenUpdateLecture_thenThrowsException() {
        Lecture lecture = publicCourse.getLectures().get(0);
        lecture.publish();
        assertThrows(CourseException.class, () -> lecture.update(new Title("수정된 강의"), "/lectures/1", "mp4"));
    }

    @Test
    @DisplayName("수정 시 title이 null이면 수정할 수 없다")
    void givenNullTitleOnUpdate_whenUpdateLecture_thenThrowsException() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        assertThrows(CourseException.class, () -> lecture.update(null, "/lectures/1", "mp4"));
    }

    @Test
    @DisplayName("강의 자료 URL이 null이면 수정할 수 없다")
    void givenNullContentUrl_whenUpdateLecture_thenThrowsException() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        assertThrows(CourseException.class, () -> lecture.update(new Title("수정된 강의"), null, "mp4"));
    }

    @Test
    @DisplayName("강의 자료 URL이 빈 문자열이면 수정할 수 없다")
    void givenBlankContentUrl_whenUpdateLecture_thenThrowsException() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        assertThrows(CourseException.class, () -> lecture.update(new Title("수정된 강의"), "  ", "mp4"));
    }

    @Test
    @DisplayName("수정 시 자료 타입이 null이면 수정할 수 없다")
    void givenNullContentType_whenUpdateLecture_thenThrowsException() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        assertThrows(CourseException.class, () -> lecture.update(new Title("수정된 강의"), "/lectures/1", null));
    }

    @Test
    @DisplayName("수정 시 자료 타입이 빈 문자열이면 수정할 수 없다")
    void givenBlankContentType_whenUpdateLecture_thenThrowsException() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        assertThrows(CourseException.class, () -> lecture.update(new Title("수정된 강의"), "/lectures/1", "   "));
    }

    @Test
    @DisplayName("자료 타입을 지정하여 강의를 수정하면 자료 타입이 변경된다")
    void givenValidContentType_whenUpdateLecture_thenContentTypeIsUpdated() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        lecture.update(new Title("수정된 강의"), "/lectures/1", "pdf");
        assertEquals("pdf", lecture.getContentType());
    }

    @Test
    @DisplayName("강의를 공개할 수 있다")
    void givenPrivateLecture_whenPublish_thenStatusIsPublic() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        lecture.unpublish();
        lecture.publish();
        assertEquals(ContentStatus.PUBLIC, lecture.getStatus());
    }

    @Test
    @DisplayName("강의를 비공개하면 상태가 PRIVATE이 된다")
    void givenPublicLecture_whenUnpublish_thenStatusIsPrivate() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        lecture.publish();
        lecture.unpublish();

        assertEquals(ContentStatus.PRIVATE, lecture.getStatus());
    }

    // --- delete ---

    @Test
    @DisplayName("강의 생성 시 삭제 플래그는 false이고 삭제일시는 null이다")
    void givenNewLecture_whenCreated_thenNotDeletedAndDeletedAtIsNull() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");

        assertFalse(lecture.isDeleted());
        assertNull(lecture.getDeletedAt());
    }

    @Test
    @DisplayName("강의를 삭제하면 삭제 플래그가 true가 되고 삭제일시가 설정된다")
    void givenLecture_whenDelete_thenDeletedFlagIsTrueAndDeletedAtIsSet() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");

        lecture.delete();

        assertTrue(lecture.isDeleted());
        assertNotNull(lecture.getDeletedAt());
    }

    @Test
    @DisplayName("이미 삭제된 강의를 다시 삭제하면 예외가 발생한다")
    void givenDeletedLecture_whenDeleteAgain_thenThrowsException() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        lecture.delete();

        assertThrows(CourseException.class, lecture::delete);
    }

    @Test
    @DisplayName("삭제된 강의를 수정하면 예외가 발생한다")
    void givenDeletedLecture_whenUpdate_thenThrowsException() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        lecture.delete();

        assertThrows(CourseException.class, () -> lecture.update(new Title("수정된 강의"), "/lectures/1", "mp4"));
    }

    @Test
    @DisplayName("삭제된 강의를 공개하면 예외가 발생한다")
    void givenDeletedLecture_whenPublish_thenThrowsException() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        lecture.unpublish();
        lecture.delete();

        assertThrows(CourseException.class, lecture::publish);
    }

    @Test
    @DisplayName("삭제된 강의를 비공개하면 예외가 발생한다")
    void givenDeletedLecture_whenUnpublish_thenThrowsException() {
        Lecture lecture = privateCourse.addLecture(new Title("강의"), "/lectures/1", "mp4");
        lecture.delete();

        assertThrows(CourseException.class, lecture::unpublish);
    }
}
