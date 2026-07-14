package com.lcs.lxp.security.refresh;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenTest {

    @Test
    @DisplayName("만료 시각이 현재보다 미래이면 isExpired는 false를 반환한다")
    void givenExpiryDateAfterNow_whenIsExpired_thenReturnsFalse() {
        RefreshToken refreshToken =
                new RefreshToken("user@example.com", "token-value", Instant.now().plusSeconds(3600));

        boolean result = refreshToken.isExpired();

        assertFalse(result);
    }

    @Test
    @DisplayName("만료 시각이 현재보다 과거이면 isExpired는 true를 반환한다")
    void givenExpiryDateBeforeNow_whenIsExpired_thenReturnsTrue() {
        RefreshToken refreshToken =
                new RefreshToken("user@example.com", "token-value", Instant.now().minusSeconds(3600));

        boolean result = refreshToken.isExpired();

        assertTrue(result);
    }
}
