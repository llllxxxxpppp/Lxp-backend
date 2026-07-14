package com.lcs.lxp.security.principal;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomUserPrincipalTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "encoded-password";

    private static List<GrantedAuthority> memberAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
    }

    // -------------------------------------------------------------------------
    // isEnabled()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("탈퇴하지 않은 회원이면 isEnabled()는 true를 반환한다")
    void givenNotDeletedMember_whenIsEnabled_thenReturnsTrue() {
        CustomUserPrincipal principal =
                new CustomUserPrincipal(USER_ID, EMAIL, PASSWORD, memberAuthorities(), false);

        assertTrue(principal.isEnabled());
    }

    @Test
    @DisplayName("탈퇴한 회원이면 isEnabled()는 false를 반환한다")
    void givenDeletedMember_whenIsEnabled_thenReturnsFalse() {
        CustomUserPrincipal principal =
                new CustomUserPrincipal(USER_ID, EMAIL, PASSWORD, memberAuthorities(), true);

        assertFalse(principal.isEnabled());
    }

    // -------------------------------------------------------------------------
    // UserDetails accessor delegation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("생성 시 전달한 값들이 각 접근자를 통해 그대로 노출된다")
    void givenConstructedPrincipal_whenAccessed_thenExposesConstructorValues() {
        List<GrantedAuthority> authorities = memberAuthorities();
        CustomUserPrincipal principal =
                new CustomUserPrincipal(USER_ID, EMAIL, PASSWORD, authorities, false);

        assertEquals(USER_ID, principal.getUserId());
        assertEquals(EMAIL, principal.getUsername());
        assertEquals(PASSWORD, principal.getPassword());
        assertEquals(authorities, principal.getAuthorities());
    }
}
