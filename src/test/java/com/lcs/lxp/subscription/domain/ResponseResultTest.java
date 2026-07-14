package com.lcs.lxp.subscription.domain;

import com.lcs.lxp.subscription.domain.model.vo.ResponseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SUB-02: ResponseResult(응답 결과) VO 검증.
 * NOT_REQUESTED(요청 전, default)/SUCCESS(성공 응답)/FAILED(실패 응답) 세 값만 갖는다.
 */
class ResponseResultTest {

    @Test
    @DisplayName("ResponseResult는 NOT_REQUESTED/SUCCESS/FAILED 세 값만 갖는다")
    void whenGetValues_thenOnlyThreeResultsExist() {
        ResponseResult[] values = ResponseResult.values();

        assertEquals(3, values.length);
    }

    @Test
    @DisplayName("NOT_REQUESTED 값이 존재한다")
    void givenNotRequestedName_whenValueOf_thenReturnsNotRequestedResult() {
        assertEquals(ResponseResult.NOT_REQUESTED, ResponseResult.valueOf("NOT_REQUESTED"));
    }

    @Test
    @DisplayName("SUCCESS 값이 존재한다")
    void givenSuccessName_whenValueOf_thenReturnsSuccessResult() {
        assertEquals(ResponseResult.SUCCESS, ResponseResult.valueOf("SUCCESS"));
    }

    @Test
    @DisplayName("FAILED 값이 존재한다")
    void givenFailedName_whenValueOf_thenReturnsFailedResult() {
        assertEquals(ResponseResult.FAILED, ResponseResult.valueOf("FAILED"));
    }

    @Test
    @DisplayName("정의되지 않은 이름으로 조회하면 예외가 발생한다")
    void givenUndefinedName_whenValueOf_thenThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ResponseResult.valueOf("UNKNOWN"));
    }
}
