package com.lcs.lxp.member.model.vo;

import java.util.Objects;

public record MemberId(Long value) {

    public MemberId {
        Objects.requireNonNull(value, "MemberId는 null일 수 없습니다.");
    }
}
