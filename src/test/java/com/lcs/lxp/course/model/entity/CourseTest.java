package com.lcs.lxp.course.model.entity;

import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.model.vo.ContentStatus;
import com.lcs.lxp.course.model.vo.InstructorId;
import com.lcs.lxp.course.model.vo.LectureId;
import com.lcs.lxp.course.model.vo.MissionId;
import com.lcs.lxp.course.model.vo.ReorderItem;
import com.lcs.lxp.course.model.vo.Sortable;
import com.lcs.lxp.course.model.vo.SortableType;
import com.lcs.lxp.course.model.vo.Title;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    @DisplayName("강좌 생성 시 생성일시는 not-null이고 수정일시는 null이다")
    void givenValidArguments_whenCreateCourse_thenCreatedAtNotNullAndUpdatedAtNull() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);

        assertNotNull(course.getCreatedAt());
        assertNull(course.getUpdatedAt());
    }

    @Test
    @DisplayName("instructorId가 null이면 강좌 생성 시 예외가 발생한다")
    void givenNullInstructorId_whenCreateCourse_thenThrowsException() {
        assertThrows(CourseException.class, () -> Course.create(null, title, "강좌 설명", null));
    }

    @Test
    @DisplayName("title이 null이면 강좌 생성 시 예외가 발생한다")
    void givenNullTitle_whenCreateCourse_thenThrowsException() {
        assertThrows(CourseException.class, () -> Course.create(instructorId, null, "강좌 설명", null));
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
        course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        course.addMission(new Title("미션"), "문제 내용");
        course.publish();

        assertThrows(CourseException.class, () -> course.update(new Title("수정된 제목"), "수정된 설명", null));
    }

    @Test
    @DisplayName("수정 시 title이 null이면 예외가 발생한다")
    void givenNullTitleOnUpdate_whenUpdate_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        assertThrows(CourseException.class, () -> course.update(null, "수정된 설명", null));
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
        course.addLecture(new Title("강의"), "/lectures/1", "mp4");
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
        course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        course.addMission(new Title("미션"), "문제 내용");
        assertDoesNotThrow(course::publish);
        assertEquals(ContentStatus.PUBLIC, course.getStatus());
    }

    @Test
    @DisplayName("비공개 처리하면 상태가 PRIVATE이 된다")
    void givenPublicCourse_whenUnpublish_thenStatusIsPrivate() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        course.addMission(new Title("미션"), "문제 내용");
        course.publish();
        course.unpublish();

        assertEquals(ContentStatus.PRIVATE, course.getStatus());
    }

    @Test
    @DisplayName("공개 상태에서 강의를 추가하면 예외가 발생한다")
    void givenPublicCourse_whenAddLecture_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        course.addMission(new Title("미션"), "문제 내용");
        course.publish();

        assertThrows(CourseException.class, () -> course.addLecture(new Title("강의2"), "/lectures/2", "mp4"));
    }

    @Test
    @DisplayName("공개 상태에서 미션을 추가하면 예외가 발생한다")
    void givenPublicCourse_whenAddMission_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        course.addMission(new Title("미션"), "문제 내용");
        course.publish();

        assertThrows(CourseException.class, () -> course.addMission(new Title("미션2"), "문제 내용"));
    }

    // --- remove (물리 삭제) ---

    @Test
    @DisplayName("비공개 강좌에서 강의를 제거할 수 있다")
    void givenPrivateCourse_whenRemoveLecture_thenLectureIsRemoved() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);

        course.removeLecture(new LectureId(10L));

        assertTrue(course.getLectures().isEmpty());
    }

    @Test
    @DisplayName("공개 상태에서 강의를 제거하면 예외가 발생한다")
    void givenPublicCourse_whenRemoveLecture_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);
        course.addMission(new Title("미션"), "문제 내용");
        course.publish();

        assertThrows(CourseException.class, () -> course.removeLecture(new LectureId(10L)));
    }

    @Test
    @DisplayName("존재하지 않는 강의를 제거하면 예외가 발생한다")
    void givenNonExistentLecture_whenRemoveLecture_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);

        assertThrows(CourseException.class, () -> course.removeLecture(new LectureId(999L)));
    }

    @Test
    @DisplayName("비공개 강좌에서 미션을 제거할 수 있다")
    void givenPrivateCourse_whenRemoveMission_thenMissionIsRemoved() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);

        course.removeMission(new MissionId(20L));

        assertTrue(course.getMissions().isEmpty());
    }

    @Test
    @DisplayName("공개 상태에서 미션을 제거하면 예외가 발생한다")
    void givenPublicCourse_whenRemoveMission_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);
        course.publish();

        assertThrows(CourseException.class, () -> course.removeMission(new MissionId(20L)));
    }

    @Test
    @DisplayName("존재하지 않는 미션을 제거하면 예외가 발생한다")
    void givenNonExistentMission_whenRemoveMission_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);

        assertThrows(CourseException.class, () -> course.removeMission(new MissionId(999L)));
    }

    @Test
    @DisplayName("삭제된 강좌에서 강의를 제거하면 예외가 발생한다")
    void givenDeletedCourse_whenRemoveLecture_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);
        course.delete();

        assertThrows(CourseException.class, () -> course.removeLecture(new LectureId(10L)));
    }

    @Test
    @DisplayName("삭제된 강좌에서 미션을 제거하면 예외가 발생한다")
    void givenDeletedCourse_whenRemoveMission_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);
        course.delete();

        assertThrows(CourseException.class, () -> course.removeMission(new MissionId(20L)));
    }

    @Test
    @DisplayName("존재하지 않는 강의를 Course를 통해 수정하면 예외가 발생한다")
    void givenLectureNotInCourse_whenUpdateLecture_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        assertThrows(CourseException.class,
                () -> course.updateLecture(new LectureId(999L), new Title("수정된 강의"), "/lectures/1", "mp4"));
    }

    @Test
    @DisplayName("Course를 통해 강의를 수정할 수 있다")
    void givenLectureInCourse_whenUpdateLectureViaCourse_thenSucceeds() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);

        assertDoesNotThrow(() -> course.updateLecture(new LectureId(10L), new Title("수정된 강의"), "/lectures/1", "mp4"));
        assertEquals("수정된 강의", course.getLectures().get(0).getTitle().getValue());
    }

    @Test
    @DisplayName("Course를 통해 강의를 공개할 수 있다")
    void givenLectureInCourse_whenPublishLectureViaCourse_thenStatusIsPublic() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);

        course.publishLecture(new LectureId(10L));
        assertEquals(ContentStatus.PUBLIC, course.getLectures().get(0).getStatus());
    }

    @Test
    @DisplayName("존재하지 않는 미션을 Course를 통해 수정하면 예외가 발생한다")
    void givenMissionNotInCourse_whenUpdateMission_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        assertThrows(CourseException.class,
                () -> course.updateMission(new MissionId(999L), new Title("수정된 미션"), "수정된 문제"));
    }

    @Test
    @DisplayName("Course를 통해 미션을 수정할 수 있다")
    void givenMissionInCourse_whenUpdateMissionViaCourse_thenSucceeds() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);

        assertDoesNotThrow(() -> course.updateMission(new MissionId(20L), new Title("수정된 미션"), "수정된 문제"));
        assertEquals("수정된 미션", course.getMissions().get(0).getTitle().getValue());
    }

    @Test
    @DisplayName("Course를 통해 미션을 공개할 수 있다")
    void givenMissionInCourse_whenPublishMissionViaCourse_thenStatusIsPublic() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);

        course.publishMission(new MissionId(20L));
        assertEquals(ContentStatus.PUBLIC, course.getMissions().get(0).getStatus());
    }

    // --- delete ---

    @Test
    @DisplayName("강좌 생성 시 삭제 플래그는 false이고 삭제일시는 null이다")
    void givenNewCourse_whenCreated_thenNotDeletedAndDeletedAtIsNull() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);

        assertFalse(course.isDeleted());
        assertNull(course.getDeletedAt());
    }

    @Test
    @DisplayName("강좌를 삭제하면 삭제 플래그가 true가 되고 삭제일시가 설정된다")
    void givenCourse_whenDelete_thenDeletedFlagIsTrueAndDeletedAtIsSet() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);

        course.delete();

        assertTrue(course.isDeleted());
        assertNotNull(course.getDeletedAt());
    }

    @Test
    @DisplayName("이미 삭제된 강좌를 다시 삭제하면 예외가 발생한다")
    void givenDeletedCourse_whenDeleteAgain_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.delete();

        assertThrows(CourseException.class, course::delete);
    }

    @Test
    @DisplayName("삭제된 강좌를 수정하면 예외가 발생한다")
    void givenDeletedCourse_whenUpdate_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.delete();

        assertThrows(CourseException.class, () -> course.update(new Title("수정된 제목"), "수정된 설명", null));
    }

    @Test
    @DisplayName("삭제된 강좌에 강의를 추가하면 예외가 발생한다")
    void givenDeletedCourse_whenAddLecture_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.delete();

        assertThrows(CourseException.class, () -> course.addLecture(new Title("강의"), "/lectures/1", "mp4"));
    }

    @Test
    @DisplayName("삭제된 강좌에 미션을 추가하면 예외가 발생한다")
    void givenDeletedCourse_whenAddMission_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.delete();

        assertThrows(CourseException.class, () -> course.addMission(new Title("미션"), "문제 내용"));
    }

    @Test
    @DisplayName("삭제된 강좌를 통해 강의를 수정하면 예외가 발생한다")
    void givenDeletedCourse_whenUpdateLecture_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);
        course.delete();

        assertThrows(CourseException.class,
                () -> course.updateLecture(new LectureId(10L), new Title("수정된 강의"), "/lectures/1", "mp4"));
    }

    @Test
    @DisplayName("삭제된 강좌를 통해 강의를 공개하면 예외가 발생한다")
    void givenDeletedCourse_whenPublishLecture_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);
        course.delete();

        assertThrows(CourseException.class, () -> course.publishLecture(new LectureId(10L)));
    }

    @Test
    @DisplayName("삭제된 강좌를 통해 강의를 비공개하면 예외가 발생한다")
    void givenDeletedCourse_whenUnpublishLecture_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);
        course.delete();

        assertThrows(CourseException.class, () -> course.unpublishLecture(new LectureId(10L)));
    }

    @Test
    @DisplayName("삭제된 강좌를 통해 미션을 수정하면 예외가 발생한다")
    void givenDeletedCourse_whenUpdateMission_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);
        course.delete();

        assertThrows(CourseException.class,
                () -> course.updateMission(new MissionId(20L), new Title("수정된 미션"), "수정된 문제"));
    }

    @Test
    @DisplayName("삭제된 강좌를 통해 미션을 공개하면 예외가 발생한다")
    void givenDeletedCourse_whenPublishMission_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);
        course.delete();

        assertThrows(CourseException.class, () -> course.publishMission(new MissionId(20L)));
    }

    @Test
    @DisplayName("삭제된 강좌를 통해 미션을 비공개하면 예외가 발생한다")
    void givenDeletedCourse_whenUnpublishMission_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);
        course.delete();

        assertThrows(CourseException.class, () -> course.unpublishMission(new MissionId(20L)));
    }

    @Test
    @DisplayName("삭제된 강좌를 공개하면 예외가 발생한다")
    void givenDeletedCourse_whenPublish_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        course.addMission(new Title("미션"), "문제 내용");
        course.delete();

        assertThrows(CourseException.class, course::publish);
    }

    @Test
    @DisplayName("삭제된 강좌를 비공개하면 예외가 발생한다")
    void givenDeletedCourse_whenUnpublish_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        course.addMission(new Title("미션"), "문제 내용");
        course.publish();
        course.delete();

        assertThrows(CourseException.class, course::unpublish);
    }

    // --- Sortable ---

    @Test
    @DisplayName("강좌에 첫 강의를 추가하면 순번은 1이다")
    void givenNewCourse_whenAddFirstLecture_thenSortOrderIsOne() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");

        assertEquals(1, lecture.getSortOrder());
    }

    @Test
    @DisplayName("강의를 추가한 후 미션을 추가하면 미션의 순번은 2가 된다")
    void givenCourseWithOneLecture_whenAddMission_thenMissionSortOrderIsTwo() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        Mission mission = course.addMission(new Title("미션"), "문제 내용");

        assertEquals(1, lecture.getSortOrder());
        assertEquals(2, mission.getSortOrder());
    }

    @Test
    @DisplayName("강의와 미션을 추가한 후 다시 강의를 추가하면 순번은 3이 된다")
    void givenCourseWithLectureAndMission_whenAddAnotherLecture_thenSortOrderIsThree() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의1"), "/lectures/1", "mp4");
        course.addMission(new Title("미션"), "문제 내용");
        Lecture secondLecture = course.addLecture(new Title("강의2"), "/lectures/2", "mp4");

        assertEquals(3, secondLecture.getSortOrder());
    }

    @Test
    @DisplayName("강좌에 강의와 미션을 여러 개 추가하면 순번이 모두 고유하다")
    void givenCourse_whenAddingMultipleLecturesAndMissions_thenAllSortOrdersAreUnique() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        course.addLecture(new Title("강의1"), "/lectures/1", "mp4");
        course.addMission(new Title("미션1"), "문제1");
        course.addLecture(new Title("강의2"), "/lectures/2", "mp4");
        course.addMission(new Title("미션2"), "문제2");
        course.addLecture(new Title("강의3"), "/lectures/3", "mp4");

        List<Sortable> sortables = new ArrayList<>();
        sortables.addAll(course.getLectures());
        sortables.addAll(course.getMissions());

        List<Integer> sortOrders = sortables.stream()
                .map(Sortable::getSortOrder)
                .collect(Collectors.toList());
        Set<Integer> uniqueSortOrders = new HashSet<>(sortOrders);

        assertEquals(sortOrders.size(), uniqueSortOrders.size());
    }

    @Test
    @DisplayName("강의를 물리 삭제한 후 다시 추가하면 순번이 기존 강의와 중복되지 않고 최댓값 + 1이 된다")
    void givenLectureRemoved_whenAddLectureAgain_thenSortOrderIsMaxPlusOneAndUnique() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture1 = course.addLecture(new Title("강의1"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture1, "id", 10L);
        Lecture lecture2 = course.addLecture(new Title("강의2"), "/lectures/2", "mp4");

        assertEquals(1, lecture1.getSortOrder());
        assertEquals(2, lecture2.getSortOrder());

        course.removeLecture(new LectureId(10L));
        Lecture lecture3 = course.addLecture(new Title("강의3"), "/lectures/3", "mp4");

        assertEquals(3, lecture3.getSortOrder());

        List<Sortable> remaining = new ArrayList<>();
        remaining.addAll(course.getLectures());
        remaining.addAll(course.getMissions());
        List<Integer> sortOrders = remaining.stream().map(Sortable::getSortOrder).collect(Collectors.toList());
        Set<Integer> uniqueSortOrders = new HashSet<>(sortOrders);

        assertEquals(sortOrders.size(), uniqueSortOrders.size());
    }

    // --- reorder ---

    @Test
    @DisplayName("강의와 미션의 순서를 요청한 순서대로 재배치하면 순번이 1부터 순서대로 재할당된다")
    void givenValidOrder_whenReorder_thenSortOrdersAreReassignedInGivenOrder() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);
        assertEquals(1, lecture.getSortOrder());
        assertEquals(2, mission.getSortOrder());

        course.reorder(List.of(
                new ReorderItem(SortableType.MISSION, 20L),
                new ReorderItem(SortableType.LECTURE, 10L)));

        assertEquals(1, mission.getSortOrder());
        assertEquals(2, lecture.getSortOrder());
    }

    @Test
    @DisplayName("다른 강좌에 속한(존재하지 않는) 강의 ID가 섞여 있으면 순서 변경 시 예외가 발생한다")
    void givenLectureIdNotBelongingToCourse_whenReorder_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);

        assertThrows(CourseException.class, () -> course.reorder(List.of(
                new ReorderItem(SortableType.LECTURE, 999L),
                new ReorderItem(SortableType.MISSION, 20L))));
    }

    @Test
    @DisplayName("다른 강좌에 속한(존재하지 않는) 미션 ID가 섞여 있으면 순서 변경 시 예외가 발생한다")
    void givenMissionIdNotBelongingToCourse_whenReorder_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);

        assertThrows(CourseException.class, () -> course.reorder(List.of(
                new ReorderItem(SortableType.LECTURE, 10L),
                new ReorderItem(SortableType.MISSION, 999L))));
    }

    @Test
    @DisplayName("강좌에 속한 강의/미션 일부만 순서 변경 대상으로 포함하면 예외가 발생한다")
    void givenPartialItemList_whenReorder_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);

        assertThrows(CourseException.class,
                () -> course.reorder(List.of(new ReorderItem(SortableType.LECTURE, 10L))));
    }

    @Test
    @DisplayName("순서 변경 대상 목록에 같은 강의가 중복되어 있으면 예외가 발생한다")
    void givenDuplicateItemInList_whenReorder_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);

        assertThrows(CourseException.class, () -> course.reorder(List.of(
                new ReorderItem(SortableType.LECTURE, 10L),
                new ReorderItem(SortableType.LECTURE, 10L))));
    }

    @Test
    @DisplayName("삭제된 강좌의 순서를 변경하면 예외가 발생한다")
    void givenDeletedCourse_whenReorder_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);
        course.delete();

        assertThrows(CourseException.class, () -> course.reorder(List.of(
                new ReorderItem(SortableType.LECTURE, 10L),
                new ReorderItem(SortableType.MISSION, 20L))));
    }

    @Test
    @DisplayName("공개 상태에서 순서를 변경하면 예외가 발생한다")
    void givenPublicCourse_whenReorder_thenThrowsException() {
        Course course = Course.create(instructorId, title, "강좌 설명", null);
        Lecture lecture = course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        ReflectionTestUtils.setField(lecture, "id", 10L);
        Mission mission = course.addMission(new Title("미션"), "문제 내용");
        ReflectionTestUtils.setField(mission, "id", 20L);
        course.publish();

        assertThrows(CourseException.class, () -> course.reorder(List.of(
                new ReorderItem(SortableType.MISSION, 20L),
                new ReorderItem(SortableType.LECTURE, 10L))));
    }

}
