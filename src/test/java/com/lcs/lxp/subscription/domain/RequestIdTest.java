package com.lcs.lxp.subscription.domain;

import com.lcs.lxp.subscription.domain.model.vo.RequestId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SUB-02: RequestId(멱등키) VO 검증.
 *
 * <p>RequestId는 UUID 값을 표현하며 null일 수 없다. 생성 시(generate())에는
 * 항상 UUIDv4 값으로 생성되지만, 생성자 자체는 버전 형식을 별도로 검증하지 않는다.
 */
class RequestIdTest {

    private static final UUID SAMPLE_UUID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @Test
    @DisplayName("값이 null이면 RequestId 생성 시 예외가 발생한다")
    void givenNullValue_whenCreateRequestId_thenThrowsException() {
        assertThrows(NullPointerException.class, () -> new RequestId(null));
    }

    @Test
    @DisplayName("유효한 UUID 값이면 RequestId가 생성되고 값이 그대로 저장된다")
    void givenValidUuid_whenCreateRequestId_thenValueIsStored() {
        RequestId requestId = new RequestId(SAMPLE_UUID);

        assertEquals(SAMPLE_UUID, requestId.value());
    }

    @Test
    @DisplayName("generate()로 생성하면 UUIDv4 값을 갖는다")
    void whenGenerate_thenValueIsUuidVersion4() {
        RequestId requestId = RequestId.generate();

        assertEquals(4, requestId.value().version());
    }

    @Test
    @DisplayName("generate()를 두 번 호출하면 서로 다른 값을 생성한다")
    void whenGenerateTwice_thenValuesAreDifferent() {
        RequestId first = RequestId.generate();
        RequestId second = RequestId.generate();

        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("같은 값으로 생성한 두 RequestId는 동등하다")
    void givenSameValue_whenCreateTwoRequestIds_thenTheyAreEqual() {
        RequestId first = new RequestId(SAMPLE_UUID);
        RequestId second = new RequestId(SAMPLE_UUID);

        assertEquals(first, second);
    }
}
