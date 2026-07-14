package com.lcs.lxp.course.model.vo;

import java.util.Objects;

/**
 * 강좌 내 강의/미션 순서 변경 요청의 개별 항목이다.
 */
public record ReorderItem(SortableType type, Long id) {

    public ReorderItem {
        Objects.requireNonNull(type, "type은 null일 수 없습니다.");
        Objects.requireNonNull(id, "id는 null일 수 없습니다.");
    }
}
