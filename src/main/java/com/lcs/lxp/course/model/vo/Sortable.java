package com.lcs.lxp.course.model.vo;

/**
 * 강좌 내에서 순서를 가지는 대상(강의, 미션)이 구현하는 인터페이스이다.
 * 강좌 단위로 고유한 순번을 가진다.
 */
@FunctionalInterface
public interface Sortable {

    int getSortOrder();
}
