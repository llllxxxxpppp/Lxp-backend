package com.lcs.lxp.security.refresh;

import com.lcs.lxp.security.exception.ExpiredJwtCustomException;
import com.lcs.lxp.security.exception.InvalidJwtCustomException;
import com.lcs.lxp.security.exception.InvalidRefreshTokenException;
import com.lcs.lxp.security.jwt.JwtTokenProvider;
import com.lcs.lxp.security.principal.CustomUserPrincipal;
import com.lcs.lxp.security.service.CustomUserDetailsService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RefreshServiceTest {

    private static final String REFRESH_TOKEN_VALUE = "refresh-token-value";
    private static final String EMAIL = "user@example.com";

    private JwtTokenProvider jwtTokenProvider;
    private RefreshTokenRepository refreshTokenRepository;
    private CustomUserDetailsService userDetailsService;
    private RefreshService refreshService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        refreshService = new RefreshService(jwtTokenProvider, refreshTokenRepository, userDetailsService);
    }

    private UserDetails buildUserDetails() {
        return new CustomUserPrincipal(
                1L,
                EMAIL,
                "encoded-password",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")),
                false);
    }

    // -------------------------------------------------------------------------
    // Valid refresh token
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 refresh token이면 CustomUserDetailsService 조회 결과 기반으로 새 access token을 반환한다")
    void givenValidRefreshToken_whenRefreshAccessToken_thenReturnsNewAccessTokenFromUserDetails() {
        RefreshToken refreshTokenEntity =
                new RefreshToken(EMAIL, REFRESH_TOKEN_VALUE, Instant.now().plusSeconds(3600));
        UserDetails userDetails = buildUserDetails();

        when(jwtTokenProvider.validateToken(REFRESH_TOKEN_VALUE)).thenReturn(true);
        when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE))
                .thenReturn(Optional.of(refreshTokenEntity));
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtTokenProvider.createAccessToken(any(Authentication.class))).thenReturn("new-access-token");

        String result = refreshService.refreshAccessToken(REFRESH_TOKEN_VALUE);

        assertEquals("new-access-token", result);

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(jwtTokenProvider).createAccessToken(authenticationCaptor.capture());
        assertEquals(EMAIL, authenticationCaptor.getValue().getName());
        verify(userDetailsService).loadUserByUsername(EMAIL);
        verify(refreshTokenRepository, never()).delete(any());
    }

    // -------------------------------------------------------------------------
    // Expired JWT refresh token
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("refresh token이 만료된 JWT이면 DB 레코드를 삭제하고 InvalidRefreshTokenException을 던진다")
    void givenExpiredJwtRefreshToken_whenRefreshAccessToken_thenDeletesRecordAndThrowsInvalidRefreshTokenException() {
        RefreshToken refreshTokenEntity =
                new RefreshToken(EMAIL, REFRESH_TOKEN_VALUE, Instant.now().plusSeconds(3600));

        when(jwtTokenProvider.validateToken(REFRESH_TOKEN_VALUE))
                .thenThrow(new ExpiredJwtCustomException("expired"));
        when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE))
                .thenReturn(Optional.of(refreshTokenEntity));

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshService.refreshAccessToken(REFRESH_TOKEN_VALUE));

        verify(refreshTokenRepository).delete(refreshTokenEntity);
        verifyNoInteractions(userDetailsService);
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }

    // -------------------------------------------------------------------------
    // Invalid JWT refresh token
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("refresh token이 무효한 JWT이면 DB 조회 없이 InvalidRefreshTokenException을 던진다")
    void givenInvalidJwtRefreshToken_whenRefreshAccessToken_thenThrowsInvalidRefreshTokenExceptionWithoutDbLookup() {
        when(jwtTokenProvider.validateToken(REFRESH_TOKEN_VALUE))
                .thenThrow(new InvalidJwtCustomException("invalid"));

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshService.refreshAccessToken(REFRESH_TOKEN_VALUE));

        verifyNoInteractions(refreshTokenRepository);
        verifyNoInteractions(userDetailsService);
    }

    // -------------------------------------------------------------------------
    // Missing DB record
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DB에 refresh token 레코드가 없으면 InvalidRefreshTokenException을 던진다")
    void givenRefreshTokenNotFoundInDatabase_whenRefreshAccessToken_thenThrowsInvalidRefreshTokenException() {
        when(jwtTokenProvider.validateToken(REFRESH_TOKEN_VALUE)).thenReturn(true);
        when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshService.refreshAccessToken(REFRESH_TOKEN_VALUE));

        verify(refreshTokenRepository, never()).delete(any());
        verifyNoInteractions(userDetailsService);
    }

    // -------------------------------------------------------------------------
    // DB record expired
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DB 레코드가 존재하지만 isExpired()가 true이면 레코드를 삭제하고 InvalidRefreshTokenException을 던진다")
    void givenDatabaseRecordIsExpired_whenRefreshAccessToken_thenDeletesRecordAndThrowsInvalidRefreshTokenException() {
        RefreshToken expiredEntity =
                new RefreshToken(EMAIL, REFRESH_TOKEN_VALUE, Instant.now().minusSeconds(3600));

        when(jwtTokenProvider.validateToken(REFRESH_TOKEN_VALUE)).thenReturn(true);
        when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE))
                .thenReturn(Optional.of(expiredEntity));

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshService.refreshAccessToken(REFRESH_TOKEN_VALUE));

        verify(refreshTokenRepository).delete(expiredEntity);
        verifyNoInteractions(userDetailsService);
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }
}
