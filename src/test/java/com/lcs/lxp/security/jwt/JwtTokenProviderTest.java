package com.lcs.lxp.security.jwt;

import com.lcs.lxp.security.exception.ExpiredJwtCustomException;
import com.lcs.lxp.security.exception.InvalidJwtCustomException;
import com.lcs.lxp.security.principal.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    private static final String SECRET_KEY =
            "4d553a82c87c2a2e0b7000d63eb926f3ef75fd528977b9c956efcec692845953";
    private static final String OTHER_SECRET_KEY =
            "9f1e2d3c4b5a69788796a5b4c3d2e1f0112233445566778899aabbccddeeff00";
    private static final long ACCESS_TOKEN_VALIDITY_MS = 3_600_000L;
    private static final long REFRESH_TOKEN_VALIDITY_MS = 864_000_000L;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = createProvider(SECRET_KEY, ACCESS_TOKEN_VALIDITY_MS, REFRESH_TOKEN_VALIDITY_MS);
    }

    private JwtTokenProvider createProvider(String secret, long accessValidityMs, long refreshValidityMs) {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secretKey", secret);
        ReflectionTestUtils.setField(provider, "accessTokenExpireTime", accessValidityMs);
        ReflectionTestUtils.setField(provider, "refreshTokenExpireTime", refreshValidityMs);
        provider.init();
        return provider;
    }

    private Authentication buildAuthentication() {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                1L,
                "user@example.com",
                "",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("ROLE_INSTRUCTOR")),
                false);

        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
    }

    // -------------------------------------------------------------------------
    // resolveToken
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Authorization 헤더가 'Bearer '로 시작하면 resolveToken은 토큰 문자열을 반환한다")
    void givenBearerAuthorizationHeader_whenResolveToken_thenReturnsTokenString() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer abc.def.ghi");

        String result = jwtTokenProvider.resolveToken(request);

        assertEquals("abc.def.ghi", result);
        verify(request).getHeader("Authorization");
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 resolveToken은 null을 반환한다")
    void givenNoAuthorizationHeader_whenResolveToken_thenReturnsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        String result = jwtTokenProvider.resolveToken(request);

        assertNull(result);
        verify(request).getHeader("Authorization");
    }

    @Test
    @DisplayName("Authorization 헤더 형식이 Bearer 스킴이 아니면 resolveToken은 null을 반환한다")
    void givenAuthorizationHeaderWithWrongScheme_whenResolveToken_thenReturnsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        String result = jwtTokenProvider.resolveToken(request);

        assertNull(result);
        verify(request).getHeader("Authorization");
    }

    // -------------------------------------------------------------------------
    // resolveRefreshToken
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("X-Refresh-Token 헤더가 있으면 resolveRefreshToken은 헤더 값을 반환한다")
    void givenXRefreshTokenHeaderPresent_whenResolveRefreshToken_thenReturnsHeaderValue() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Refresh-Token")).thenReturn("refresh-token-value");

        String result = jwtTokenProvider.resolveRefreshToken(request);

        assertEquals("refresh-token-value", result);
        verify(request).getHeader("X-Refresh-Token");
    }

    @Test
    @DisplayName("X-Refresh-Token 헤더가 없으면 resolveRefreshToken은 null을 반환한다")
    void givenXRefreshTokenHeaderAbsent_whenResolveRefreshToken_thenReturnsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Refresh-Token")).thenReturn(null);

        String result = jwtTokenProvider.resolveRefreshToken(request);

        assertNull(result);
        verify(request).getHeader("X-Refresh-Token");
    }

    // -------------------------------------------------------------------------
    // createAccessToken + getAuthentication
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createAccessToken으로 생성한 토큰을 getAuthentication으로 복원하면 username/roles/userId가 일치한다")
    void givenAccessTokenCreatedFromAuthentication_whenGetAuthentication_thenUsernameRolesAndUserIdMatch() {
        Authentication authentication = buildAuthentication();

        String token = jwtTokenProvider.createAccessToken(authentication);
        assertNotNull(token);

        Authentication restored = jwtTokenProvider.getAuthentication(token);

        assertEquals("user@example.com", restored.getName());
        assertTrue(restored.getPrincipal() instanceof CustomUserPrincipal);

        CustomUserPrincipal restoredPrincipal = (CustomUserPrincipal) restored.getPrincipal();
        assertEquals(1L, restoredPrincipal.getUserId());

        List<String> restoredRoles = restored.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        assertEquals(List.of("ROLE_MEMBER", "ROLE_INSTRUCTOR"), restoredRoles);
    }

    // -------------------------------------------------------------------------
    // validateToken
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 토큰을 검증하면 validateToken은 true를 반환한다")
    void givenValidToken_whenValidateToken_thenReturnsTrue() {
        String token = jwtTokenProvider.createAccessToken(buildAuthentication());

        boolean result = jwtTokenProvider.validateToken(token);

        assertTrue(result);
    }

    @Test
    @DisplayName("만료된 토큰을 검증하면 ExpiredJwtCustomException이 발생한다")
    void givenExpiredToken_whenValidateToken_thenThrowsExpiredJwtCustomException() {
        JwtTokenProvider expiringProvider = createProvider(SECRET_KEY, -1_000L, REFRESH_TOKEN_VALIDITY_MS);
        String expiredToken = expiringProvider.createAccessToken(buildAuthentication());

        assertThrows(ExpiredJwtCustomException.class, () -> jwtTokenProvider.validateToken(expiredToken));
    }

    @Test
    @DisplayName("형식이 올바르지 않은 토큰을 검증하면 InvalidJwtCustomException이 발생한다")
    void givenMalformedToken_whenValidateToken_thenThrowsInvalidJwtCustomException() {
        String malformedToken = "invalid.token.value";

        assertThrows(InvalidJwtCustomException.class, () -> jwtTokenProvider.validateToken(malformedToken));
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰을 검증하면 InvalidJwtCustomException이 발생한다")
    void givenTokenSignedWithDifferentKey_whenValidateToken_thenThrowsInvalidJwtCustomException() {
        JwtTokenProvider otherProvider =
                createProvider(OTHER_SECRET_KEY, ACCESS_TOKEN_VALIDITY_MS, REFRESH_TOKEN_VALIDITY_MS);
        String tokenSignedWithOtherKey = otherProvider.createAccessToken(buildAuthentication());

        assertThrows(
                InvalidJwtCustomException.class,
                () -> jwtTokenProvider.validateToken(tokenSignedWithOtherKey));
    }

    // -------------------------------------------------------------------------
    // createRefreshToken
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createRefreshToken으로 생성한 토큰은 서명이 유효하여 validateToken이 true를 반환한다")
    void givenRefreshTokenCreated_whenValidateToken_thenReturnsTrue() {
        String refreshToken = jwtTokenProvider.createRefreshToken();

        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
    }
}
