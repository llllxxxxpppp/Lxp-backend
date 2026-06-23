package com.lcs.lxp.course.model.vo;

import java.util.Objects;

public record InstructorId(Long value) {

    public InstructorId {
        Objects.requireNonNull(value, "InstructorId는 null일 수 없습니다.");
    }
}
