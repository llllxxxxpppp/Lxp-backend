package com.lcs.lxp.course.model;

import java.util.Objects;

public record LectureId(Long value) {

    public LectureId {
        Objects.requireNonNull(value, "LectureId는 null일 수 없습니다.");
    }
}
