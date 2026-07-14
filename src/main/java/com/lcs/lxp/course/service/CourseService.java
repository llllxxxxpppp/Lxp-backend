package com.lcs.lxp.course.service;

import com.lcs.lxp.course.dto.response.CourseDetailResponse;
import com.lcs.lxp.course.dto.response.CoursePageResponse;
import com.lcs.lxp.course.dto.response.CourseSummaryResponse;
import com.lcs.lxp.course.exception.CourseException;
import com.lcs.lxp.course.model.entity.Course;
import com.lcs.lxp.course.model.vo.ContentStatus;
import com.lcs.lxp.course.model.vo.InstructorId;
import com.lcs.lxp.course.model.vo.LectureId;
import com.lcs.lxp.course.model.vo.MissionId;
import com.lcs.lxp.course.model.vo.ReorderItem;
import com.lcs.lxp.course.model.vo.SortableType;
import com.lcs.lxp.course.model.vo.Title;
import com.lcs.lxp.course.repository.CourseRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public CoursePageResponse getCourses(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Course> result;
        if (keyword == null || keyword.isBlank()) {
            result = courseRepository.findAllByStatus(ContentStatus.PUBLIC, pageable);
        } else {
            result = courseRepository.findByStatusAndTitleKeyword(ContentStatus.PUBLIC, keyword, pageable);
        }
        return CoursePageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public CourseSummaryResponse getCourseSummary(Long courseId) {
        return CourseSummaryResponse.from(getCourse(courseId));
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long courseId) {
        return CourseDetailResponse.from(getCourse(courseId));
    }

    public void createCourse(Long instructorId, String title, String description, String thumbnailUrl) {
        Course course = Course.create(new InstructorId(instructorId), new Title(title), description, thumbnailUrl);
        courseRepository.save(course);
    }

    public void updateCourse(Long courseId, String newTitle, String description, String thumbnailUrl) {
        Course course = getCourse(courseId);
        course.update(new Title(newTitle), description, thumbnailUrl);
    }

    public void publishCourse(Long courseId) {
        Course course = getCourse(courseId);
        course.publish();
    }

    public void unpublishCourse(Long courseId) {
        Course course = getCourse(courseId);
        course.unpublish();
    }

    public void addLecture(Long courseId, String title, String contentUrl, String contentType) {
        getCourse(courseId).addLecture(new Title(title), contentUrl, contentType);
    }

    public void publishLecture(Long courseId, Long lectureId) {
        getCourse(courseId).publishLecture(new LectureId(lectureId));
    }

    public void unpublishLecture(Long courseId, Long lectureId) {
        getCourse(courseId).unpublishLecture(new LectureId(lectureId));
    }

    public void addMission(Long courseId, String title, String content) {
        getCourse(courseId).addMission(new Title(title), content);
    }

    public void publishMission(Long courseId, Long missionId) {
        getCourse(courseId).publishMission(new MissionId(missionId));
    }

    public void unpublishMission(Long courseId, Long missionId) {
        getCourse(courseId).unpublishMission(new MissionId(missionId));
    }

    public void deleteCourse(Long courseId) {
        getCourse(courseId).delete();
    }

    public void deleteLecture(Long courseId, Long lectureId) {
        getCourse(courseId).deleteLecture(new LectureId(lectureId));
    }

    public void deleteMission(Long courseId, Long missionId) {
        getCourse(courseId).deleteMission(new MissionId(missionId));
    }

    public void reorderItems(Long courseId, List<String> itemTypes, List<Long> itemIds) {
        if (itemTypes == null || itemIds == null || itemTypes.size() != itemIds.size()) {
            throw new CourseException("순서 변경 대상 항목 목록이 올바르지 않습니다.");
        }
        List<ReorderItem> orderedItems = new ArrayList<>();
        for (int i = 0; i < itemTypes.size(); i++) {
            orderedItems.add(new ReorderItem(SortableType.valueOf(itemTypes.get(i)), itemIds.get(i)));
        }
        getCourse(courseId).reorder(orderedItems);
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException("강좌를 찾을 수 없습니다."));
    }
}
