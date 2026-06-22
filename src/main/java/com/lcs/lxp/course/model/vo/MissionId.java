package com.lcs.lxp.course.model.vo;

import java.util.Objects;

public record MissionId(Long value) {

    public MissionId {
        Objects.requireNonNull(value, "MissionId는 null일 수 없습니다.");
    }
}
