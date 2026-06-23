package com.lcs.lxp.course.dto.response;

import com.lcs.lxp.course.model.entity.Lecture;

public record LectureResponse(Long lectureId, String title, String status) {

    public static LectureResponse from(Lecture lecture) {
        return new LectureResponse(
                lecture.getId().value(),
                lecture.getTitle().getValue(),
                lecture.getStatus().name());
    }
}
