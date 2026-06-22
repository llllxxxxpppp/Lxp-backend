package com.lcs.lxp.course.dto.response;

import com.lcs.lxp.course.model.entity.Course;
import java.util.List;

public record CourseDetailResponse(
        Long courseId,
        Long instructorId,
        String title,
        String status,
        List<LectureResponse> lectures,
        List<MissionResponse> missions) {

    public static CourseDetailResponse from(Course course) {
        List<LectureResponse> lectures = course.getLectures().stream()
                .map(LectureResponse::from)
                .toList();
        List<MissionResponse> missions = course.getMissions().stream()
                .map(MissionResponse::from)
                .toList();
        return new CourseDetailResponse(
                course.getId().value(),
                course.getInstructorId().value(),
                course.getTitle().getValue(),
                course.getStatus().name(),
                lectures,
                missions);
    }
}
