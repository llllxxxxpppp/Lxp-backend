package com.lcs.lxp.course.model.entity;

import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.model.vo.ContentStatus;
import com.lcs.lxp.course.model.vo.InstructorId;
import com.lcs.lxp.course.model.vo.Sortable;
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

class MissionTest {

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
    @DisplayName("미션은 공개 상태로 생성된다")
    void givenPrivateCourse_whenAddMission_thenStatusIsPublic() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        assertEquals(ContentStatus.PUBLIC, mission.getStatus());
    }

    @Test
    @DisplayName("미션 생성 시 생성일시는 not-null이고 수정일시는 null이다")
    void givenValidArguments_whenAddMission_thenCreatedAtNotNullAndUpdatedAtNull() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");

        assertNotNull(mission.getCreatedAt());
        assertNull(mission.getUpdatedAt());
    }

    @Test
    @DisplayName("강좌가 비공개이면 미션이 공개 상태여도 수정할 수 있다")
    void givenPrivateCourseAndPublicMission_whenUpdateMission_thenSucceeds() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        mission.publish();
        assertDoesNotThrow(() -> mission.update(new Title("수정된 미션"), "수정된 문제 내용"));
    }

    @Test
    @DisplayName("강좌가 비공개이면 미션이 비공개 상태여도 수정할 수 있다")
    void givenPrivateCourseAndPrivateMission_whenUpdateMission_thenSucceeds() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        mission.unpublish();
        assertDoesNotThrow(() -> mission.update(new Title("수정된 미션"), "수정된 문제 내용"));
    }

    @Test
    @DisplayName("강좌가 공개이고 미션이 비공개이면 수정할 수 있다")
    void givenPublicCourseAndPrivateMission_whenUpdateMission_thenSucceeds() {
        Mission mission = publicCourse.getMissions().get(0);
        mission.unpublish();
        assertDoesNotThrow(() -> mission.update(new Title("수정된 미션"), "수정된 문제 내용"));
    }

    @Test
    @DisplayName("강좌가 공개이고 미션도 공개이면 수정할 수 없다")
    void givenPublicCourseAndPublicMission_whenUpdateMission_thenThrowsException() {
        Mission mission = publicCourse.getMissions().get(0);
        mission.publish();
        assertThrows(CourseException.class, () -> mission.update(new Title("수정된 미션"), "수정된 문제 내용"));
    }

    @Test
    @DisplayName("course가 null이면 미션을 생성할 수 없다")
    void givenNullCourse_whenCreateMission_thenThrowsException() {
        assertThrows(CourseException.class, () -> Mission.create(null, new Title("미션"), "문제 내용", 1));
    }

    @Test
    @DisplayName("title이 null이면 미션을 생성할 수 없다")
    void givenNullTitle_whenCreateMission_thenThrowsException() {
        assertThrows(CourseException.class, () -> Mission.create(privateCourse, null, "문제 내용", 1));
    }

    @Test
    @DisplayName("문제 내용이 null이면 미션을 생성할 수 없다")
    void givenNullContent_whenAddMission_thenThrowsException() {
        assertThrows(CourseException.class, () -> privateCourse.addMission(new Title("미션"), null));
    }

    @Test
    @DisplayName("문제 내용이 빈 문자열이면 미션을 생성할 수 없다")
    void givenBlankContent_whenAddMission_thenThrowsException() {
        assertThrows(CourseException.class, () -> privateCourse.addMission(new Title("미션"), "  "));
    }

    @Test
    @DisplayName("문제 내용이 4096자를 초과하면 미션을 생성할 수 없다")
    void givenTooLongContent_whenAddMission_thenThrowsException() {
        String tooLong = "a".repeat(4097);
        assertThrows(CourseException.class, () -> privateCourse.addMission(new Title("미션"), tooLong));
    }

    @Test
    @DisplayName("수정 시 title이 null이면 미션을 수정할 수 없다")
    void givenNullTitleOnUpdate_whenUpdateMission_thenThrowsException() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        assertThrows(CourseException.class, () -> mission.update(null, "수정된 문제 내용"));
    }

    @Test
    @DisplayName("문제 내용이 null이면 미션을 수정할 수 없다")
    void givenNullContent_whenUpdateMission_thenThrowsException() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        assertThrows(CourseException.class, () -> mission.update(new Title("수정된 미션"), null));
    }

    @Test
    @DisplayName("문제 내용이 빈 문자열이면 미션을 수정할 수 없다")
    void givenBlankContent_whenUpdateMission_thenThrowsException() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        assertThrows(CourseException.class, () -> mission.update(new Title("수정된 미션"), "  "));
    }

    @Test
    @DisplayName("문제 내용이 4096자를 초과하면 미션을 수정할 수 없다")
    void givenTooLongContent_whenUpdateMission_thenThrowsException() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        String tooLong = "a".repeat(4097);
        assertThrows(CourseException.class, () -> mission.update(new Title("수정된 미션"), tooLong));
    }

    @Test
    @DisplayName("미션을 공개할 수 있다")
    void givenPrivateMission_whenPublish_thenStatusIsPublic() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        mission.unpublish();
        mission.publish();
        assertEquals(ContentStatus.PUBLIC, mission.getStatus());
    }

    @Test
    @DisplayName("미션을 비공개하면 상태가 PRIVATE이 된다")
    void givenPublicMission_whenUnpublish_thenStatusIsPrivate() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        mission.publish();
        mission.unpublish();

        assertEquals(ContentStatus.PRIVATE, mission.getStatus());
    }

    // --- delete ---

    @Test
    @DisplayName("미션 생성 시 삭제 플래그는 false이고 삭제일시는 null이다")
    void givenNewMission_whenCreated_thenNotDeletedAndDeletedAtIsNull() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");

        assertFalse(mission.isDeleted());
        assertNull(mission.getDeletedAt());
    }

    @Test
    @DisplayName("미션을 삭제하면 삭제 플래그가 true가 되고 삭제일시가 설정된다")
    void givenMission_whenDelete_thenDeletedFlagIsTrueAndDeletedAtIsSet() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");

        mission.delete();

        assertTrue(mission.isDeleted());
        assertNotNull(mission.getDeletedAt());
    }

    @Test
    @DisplayName("이미 삭제된 미션을 다시 삭제하면 예외가 발생한다")
    void givenDeletedMission_whenDeleteAgain_thenThrowsException() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        mission.delete();

        assertThrows(CourseException.class, mission::delete);
    }

    @Test
    @DisplayName("삭제된 미션을 수정하면 예외가 발생한다")
    void givenDeletedMission_whenUpdate_thenThrowsException() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        mission.delete();

        assertThrows(CourseException.class, () -> mission.update(new Title("수정된 미션"), "수정된 문제 내용"));
    }

    @Test
    @DisplayName("삭제된 미션을 공개하면 예외가 발생한다")
    void givenDeletedMission_whenPublish_thenThrowsException() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        mission.unpublish();
        mission.delete();

        assertThrows(CourseException.class, mission::publish);
    }

    @Test
    @DisplayName("삭제된 미션을 비공개하면 예외가 발생한다")
    void givenDeletedMission_whenUnpublish_thenThrowsException() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        mission.delete();

        assertThrows(CourseException.class, mission::unpublish);
    }

    // --- Sortable ---

    @Test
    @DisplayName("미션은 Sortable 인터페이스를 구현한다")
    void givenMission_whenCheckType_thenIsInstanceOfSortable() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        assertTrue(mission instanceof Sortable);
    }

    @Test
    @DisplayName("미션 생성 시 지정한 순번이 설정된다")
    void givenSortOrder_whenCreateMission_thenSortOrderIsSet() {
        Mission mission = Mission.create(privateCourse, new Title("미션"), "문제 내용", 7);
        assertEquals(7, mission.getSortOrder());
    }
}
