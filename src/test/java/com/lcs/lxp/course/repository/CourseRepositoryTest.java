package com.lcs.lxp.course.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lcs.lxp.course.model.entity.Course;
import com.lcs.lxp.course.model.vo.ContentStatus;
import com.lcs.lxp.course.model.vo.InstructorId;
import com.lcs.lxp.course.model.vo.Title;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * COURSE-08a: 강사 ID + PUBLIC 상태 기준 강좌 조회 쿼리 검증.
 * 인스턴스별로 seed data(demo-data.sql)와 충돌하지 않도록 seed에 없는
 * 강사 ID(500번대)를 사용한다.
 */
@DataJpaTest
class CourseRepositoryTest {

    private static final long INSTRUCTOR_A = 501L;
    private static final long INSTRUCTOR_B = 502L;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @DisplayName("강사 ID와 PUBLIC 상태로 조회하면 해당 강사가 소유한 공개 강좌만 반환한다")
    void givenMixedCoursesAcrossInstructorsAndStatuses_whenFindAllByInstructorIdAndStatus_thenReturnsOnlyMatchingInstructorPublicCourses() {
        Course publicCourse1 = createPublishedCourse(INSTRUCTOR_A, "공개 강좌 1");
        Course publicCourse2 = createPublishedCourse(INSTRUCTOR_A, "공개 강좌 2");
        Course privateCourseOfSameInstructor = Course.create(
                new InstructorId(INSTRUCTOR_A), new Title("비공개 강좌"), "설명", null);
        Course otherInstructorPublicCourse = createPublishedCourse(INSTRUCTOR_B, "다른 강사 공개 강좌");
        courseRepository.saveAll(
                List.of(publicCourse1, publicCourse2, privateCourseOfSameInstructor, otherInstructorPublicCourse));
        courseRepository.flush();

        List<Course> result = courseRepository.findAllByInstructorIdAndStatusAndDeletedAtIsNull(
                INSTRUCTOR_A, ContentStatus.PUBLIC);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(c -> c.getInstructorId().value().equals(INSTRUCTOR_A)));
        assertTrue(result.stream().allMatch(c -> c.getStatus() == ContentStatus.PUBLIC));
        List<String> titles = result.stream().map(c -> c.getTitle().getValue()).toList();
        assertTrue(titles.containsAll(List.of("공개 강좌 1", "공개 강좌 2")));
    }

    @Test
    @DisplayName("해당 강사에게 공개 강좌가 없으면 빈 목록을 반환한다")
    void givenInstructorWithNoPublicCourses_whenFindAllByInstructorIdAndStatus_thenReturnsEmptyList() {
        Course privateCourse = Course.create(new InstructorId(INSTRUCTOR_A), new Title("비공개 강좌"), "설명", null);
        courseRepository.saveAndFlush(privateCourse);

        List<Course> result = courseRepository.findAllByInstructorIdAndStatusAndDeletedAtIsNull(
                INSTRUCTOR_A, ContentStatus.PUBLIC);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("PUBLIC 상태이면서 soft delete된 강좌는 조회 결과에서 제외되고 정상 공개 강좌만 반환한다")
    void givenSoftDeletedPublicCourseAndNormalPublicCourse_whenFindAllByInstructorIdAndStatus_thenExcludesDeletedCourse() {
        final long instructorC = 503L;
        Course normalPublicCourse = createPublishedCourse(instructorC, "정상 공개 강좌");
        Course deletedPublicCourse = createPublishedCourse(instructorC, "삭제된 공개 강좌");
        deletedPublicCourse.delete();
        courseRepository.saveAll(List.of(normalPublicCourse, deletedPublicCourse));
        courseRepository.flush();

        List<Course> result = courseRepository.findAllByInstructorIdAndStatusAndDeletedAtIsNull(
                instructorC, ContentStatus.PUBLIC);

        assertEquals(1, result.size());
        assertEquals("정상 공개 강좌", result.get(0).getTitle().getValue());
        assertTrue(result.stream().noneMatch(c -> "삭제된 공개 강좌".equals(c.getTitle().getValue())));
    }

    private Course createPublishedCourse(long instructorId, String title) {
        Course course = Course.create(new InstructorId(instructorId), new Title(title), "설명", null);
        course.addLecture(new Title("강의"), "/lectures/1", "mp4");
        course.addMission(new Title("미션"), "문제 내용");
        course.publish();
        return course;
    }
}
