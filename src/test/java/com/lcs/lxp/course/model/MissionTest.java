package com.lcs.lxp.course.model;

import com.lcs.lxp.course.exception.CourseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionTest {

    private Course privateCourse;
    private Course publicCourse;

    @BeforeEach
    void setUp() {
        privateCourse = Course.create(new InstructorId(1L), new Title("강좌"));
        publicCourse = Course.create(new InstructorId(1L), new Title("강좌"));
        publicCourse.addLecture(new Title("강의"));
        publicCourse.addMission(new Title("미션"));
        publicCourse.publish();
    }

    @Test
    @DisplayName("미션은 비공개 상태로 생성된다")
    void givenPrivateCourse_whenAddMission_thenStatusIsPrivate() {
        Mission mission = privateCourse.addMission(new Title("미션"));
        assertEquals(ContentStatus.PRIVATE, mission.getStatus());
    }

    @Test
    @DisplayName("미션 생성 시 삭제 상태가 아니다")
    void givenPrivateCourse_whenAddMission_thenNotDeleted() {
        Mission mission = privateCourse.addMission(new Title("미션"));
        assertTrue(!mission.isDeleted());
    }

    @Test
    @DisplayName("강좌가 비공개이면 미션이 공개 상태여도 수정할 수 있다")
    void givenPrivateCourseAndPublicMission_whenUpdateMission_thenSucceeds() {
        Mission mission = privateCourse.addMission(new Title("미션"));
        mission.publish();
        assertDoesNotThrow(() -> mission.update(new Title("수정된 미션")));
    }

    @Test
    @DisplayName("강좌가 비공개이면 미션이 비공개 상태여도 수정할 수 있다")
    void givenPrivateCourseAndPrivateMission_whenUpdateMission_thenSucceeds() {
        Mission mission = privateCourse.addMission(new Title("미션"));
        assertDoesNotThrow(() -> mission.update(new Title("수정된 미션")));
    }

    @Test
    @DisplayName("강좌가 공개이고 미션이 비공개이면 수정할 수 있다")
    void givenPublicCourseAndPrivateMission_whenUpdateMission_thenSucceeds() {
        Mission mission = publicCourse.getMissions().get(0);
        assertDoesNotThrow(() -> mission.update(new Title("수정된 미션")));
    }

    @Test
    @DisplayName("강좌가 공개이고 미션도 공개이면 수정할 수 없다")
    void givenPublicCourseAndPublicMission_whenUpdateMission_thenThrowsException() {
        Mission mission = publicCourse.getMissions().get(0);
        mission.publish();
        assertThrows(CourseException.class, () -> mission.update(new Title("수정된 미션")));
    }

    @Test
    @DisplayName("미션을 공개할 수 있다")
    void givenPrivateMission_whenPublish_thenStatusIsPublic() {
        Mission mission = privateCourse.addMission(new Title("미션"));
        mission.publish();
        assertEquals(ContentStatus.PUBLIC, mission.getStatus());
    }

    @Test
    @DisplayName("미션을 비공개하면 삭제 상태가 된다")
    void givenPublicMission_whenUnpublish_thenStatusIsPrivateAndDeleted() {
        Mission mission = privateCourse.addMission(new Title("미션"));
        mission.publish();
        mission.unpublish();

        assertEquals(ContentStatus.PRIVATE, mission.getStatus());
        assertTrue(mission.isDeleted());
    }
}
