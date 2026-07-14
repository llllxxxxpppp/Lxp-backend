package com.lcs.lxp.security.jwt;

import com.lcs.lxp.security.exception.ExpiredJwtCustomException;
import com.lcs.lxp.security.exception.InvalidJwtCustomException;
import com.lcs.lxp.security.exception.InvalidRefreshTokenException;
import com.lcs.lxp.security.principal.CustomUserPrincipal;
import com.lcs.lxp.security.refresh.RefreshService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtTokenProvider jwtTokenProvider;
    private RefreshService refreshService;
    private JwtAuthenticationFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        refreshService = mock(RefreshService.class);
        filter = new JwtAuthenticationFilter(jwtTokenProvider, refreshService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication buildAuthentication(String username) {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                1L,
                username,
                "",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")),
                false);

        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
    }

    private void seedExistingAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(buildAuthentication("previous@example.com"));
    }

    // -------------------------------------------------------------------------
    // Valid access token
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 access token일 때 doFilterInternal은 SecurityContext에 인증 정보를 설정한다")
    void givenValidAccessToken_whenDoFilterInternal_thenSecurityContextIsSetWithAuthentication() throws Exception {
        Authentication authentication = buildAuthentication("user@example.com");
        when(jwtTokenProvider.resolveToken(request)).thenReturn("valid-access-token");
        when(jwtTokenProvider.validateToken("valid-access-token")).thenReturn(true);
        when(jwtTokenProvider.getAuthentication("valid-access-token")).thenReturn(authentication);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // Expired access token + refresh flow
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("access token 만료 + 유효한 refresh token일 때 재발급 후 응답 헤더와 SecurityContext가 갱신된다")
    void givenExpiredAccessTokenAndValidRefreshToken_whenDoFilterInternal_thenReissuesAndUpdatesContext()
            throws Exception {
        Authentication newAuthentication = buildAuthentication("user@example.com");
        when(jwtTokenProvider.resolveToken(request)).thenReturn("expired-access-token");
        when(jwtTokenProvider.validateToken("expired-access-token"))
                .thenThrow(new ExpiredJwtCustomException("expired"));
        when(jwtTokenProvider.resolveRefreshToken(request)).thenReturn("valid-refresh-token");
        when(refreshService.refreshAccessToken("valid-refresh-token")).thenReturn("new-access-token");
        when(jwtTokenProvider.getAuthentication("new-access-token")).thenReturn(newAuthentication);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(newAuthentication, SecurityContextHolder.getContext().getAuthentication());
        verify(response).setHeader("New-Access-Token", "new-access-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("access token 만료 + refresh token이 없으면 SecurityContext가 비워진다")
    void givenExpiredAccessTokenAndNoRefreshToken_whenDoFilterInternal_thenSecurityContextIsCleared()
            throws Exception {
        seedExistingAuthentication();
        when(jwtTokenProvider.resolveToken(request)).thenReturn("expired-access-token");
        when(jwtTokenProvider.validateToken("expired-access-token"))
                .thenThrow(new ExpiredJwtCustomException("expired"));
        when(jwtTokenProvider.resolveRefreshToken(request)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(refreshService);
    }

    @Test
    @DisplayName("access token 만료 + refresh token 재발급 실패(InvalidRefreshTokenException)이면 SecurityContext가 비워진다")
    void givenExpiredAccessTokenAndRefreshFailsWithInvalidRefreshTokenException_whenDoFilterInternal_thenSecurityContextIsCleared()
            throws Exception {
        seedExistingAuthentication();
        when(jwtTokenProvider.resolveToken(request)).thenReturn("expired-access-token");
        when(jwtTokenProvider.validateToken("expired-access-token"))
                .thenThrow(new ExpiredJwtCustomException("expired"));
        when(jwtTokenProvider.resolveRefreshToken(request)).thenReturn("invalid-refresh-token");
        when(refreshService.refreshAccessToken("invalid-refresh-token"))
                .thenThrow(new InvalidRefreshTokenException("invalid refresh token"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setHeader(any(), any());
    }

    @Test
    @DisplayName("access token 만료 + refresh 처리 중 예상치 못한 예외가 발생하면 SecurityContext가 비워진다")
    void givenExpiredAccessTokenAndRefreshThrowsUnexpectedException_whenDoFilterInternal_thenSecurityContextIsCleared()
            throws Exception {
        seedExistingAuthentication();
        when(jwtTokenProvider.resolveToken(request)).thenReturn("expired-access-token");
        when(jwtTokenProvider.validateToken("expired-access-token"))
                .thenThrow(new ExpiredJwtCustomException("expired"));
        when(jwtTokenProvider.resolveRefreshToken(request)).thenReturn("some-refresh-token");
        when(refreshService.refreshAccessToken("some-refresh-token"))
                .thenThrow(new IllegalStateException("unexpected failure"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setHeader(any(), any());
    }

    // -------------------------------------------------------------------------
    // Invalid access token
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("access token이 무효(InvalidJwtCustomException)이면 SecurityContext가 비워진다")
    void givenInvalidAccessToken_whenDoFilterInternal_thenSecurityContextIsCleared() throws Exception {
        seedExistingAuthentication();
        when(jwtTokenProvider.resolveToken(request)).thenReturn("malformed-access-token");
        when(jwtTokenProvider.validateToken("malformed-access-token"))
                .thenThrow(new InvalidJwtCustomException("invalid"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(refreshService);
    }
}
