package com.lcs.lxp.course.model;

import java.util.Objects;

public record CourseId(Long value) {

    public CourseId {
        Objects.requireNonNull(value, "CourseId는 null일 수 없습니다.");
    }
}
