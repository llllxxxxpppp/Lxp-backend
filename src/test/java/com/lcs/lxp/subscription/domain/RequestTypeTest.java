package com.lcs.lxp.subscription.domain;

import com.lcs.lxp.subscription.domain.model.vo.RequestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SUB-02: RequestType(요청 타입) VO 검증. PAYMENT(결제)/REFUND(환불) 두 값만 갖는다.
 */
class RequestTypeTest {

    @Test
    @DisplayName("RequestType은 PAYMENT와 REFUND 두 값만 갖는다")
    void whenGetValues_thenOnlyPaymentAndRefundExist() {
        RequestType[] values = RequestType.values();

        assertEquals(2, values.length);
    }

    @Test
    @DisplayName("PAYMENT 값이 존재한다")
    void givenPaymentName_whenValueOf_thenReturnsPaymentType() {
        assertEquals(RequestType.PAYMENT, RequestType.valueOf("PAYMENT"));
    }

    @Test
    @DisplayName("REFUND 값이 존재한다")
    void givenRefundName_whenValueOf_thenReturnsRefundType() {
        assertEquals(RequestType.REFUND, RequestType.valueOf("REFUND"));
    }

    @Test
    @DisplayName("정의되지 않은 이름으로 조회하면 예외가 발생한다")
    void givenUndefinedName_whenValueOf_thenThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> RequestType.valueOf("UNKNOWN"));
    }
}
