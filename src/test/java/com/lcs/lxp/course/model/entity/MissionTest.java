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
import static org.junit.jupiter.api.Assertions.assertThrows;

class MissionTest {

    private Course privateCourse;
    private Course publicCourse;

    @BeforeEach
    void setUp() {
        privateCourse = Course.create(new InstructorId(1L), new Title("강좌"), "강좌 설명", null);
        publicCourse = Course.create(new InstructorId(1L), new Title("강좌"), "강좌 설명", null);
        publicCourse.addLecture(new Title("강의"), "/lectures/1");
        publicCourse.addMission(new Title("미션"), "문제 내용");
        publicCourse.publish();
    }

    @Test
    @DisplayName("미션은 비공개 상태로 생성된다")
    void givenPrivateCourse_whenAddMission_thenStatusIsPrivate() {
        Mission mission = privateCourse.addMission(new Title("미션"), "문제 내용");
        assertEquals(ContentStatus.PRIVATE, mission.getStatus());
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
        assertDoesNotThrow(() -> mission.update(new Title("수정된 미션"), "수정된 문제 내용"));
    }

    @Test
    @DisplayName("강좌가 공개이고 미션이 비공개이면 수정할 수 있다")
    void givenPublicCourseAndPrivateMission_whenUpdateMission_thenSucceeds() {
        Mission mission = publicCourse.getMissions().get(0);
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
        assertThrows(CourseException.class, () -> Mission.create(null, new Title("미션"), "문제 내용"));
    }

    @Test
    @DisplayName("title이 null이면 미션을 생성할 수 없다")
    void givenNullTitle_whenCreateMission_thenThrowsException() {
        assertThrows(CourseException.class, () -> Mission.create(privateCourse, null, "문제 내용"));
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
}
