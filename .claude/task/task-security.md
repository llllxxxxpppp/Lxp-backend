# task-security.md

Security(공통 인프라) 세부 작업 파일이다. 상태의 원본은 `.claude/TASK.md` 요약표이며, 본 파일에는 상태를 중복 기록하지 않는다.

도출 근거: `.claude/domain/SECURITY.md` + `.claude/ARCHITECTURE.md`.
대조 근거: `.claude/PROGRESS.md`에 security 패키지 단위 테스트 관련 기록 없음(대조 결과 전부 ⚪ 대기).

발단: MEMBER-02 재구현(2026-07-13) 완료 후 `./gradlew check` 실행 중 전역 라인 커버리지 79%(목표 80%, `test-code-runner-agent` 기준) 발견. 원인 분석 결과 `security.jwt`/`security.refresh`/`security.principal`/`security.service`/`security.exception` 패키지에 단위 테스트가 전혀 없음을 확인(`SecurityConfigTest` 1개만 존재). 사용자 결정(2026-07-13)으로 Security를 `TASK.md` 로드맵에 경량 관리 단위로 추가하고 아래 작업으로 등록.

---

## [SEC-01] JWT 토큰 발급/파싱 단위 테스트

**설명**: `JwtTokenProvider`의 토큰 추출(`resolveToken`/`resolveRefreshToken`), 생성(`createAccessToken`/`createRefreshToken`), 검증/파싱(`validateToken`/`getAuthentication`) 및 예외 발생(만료/무효) 경로에 대한 단위 테스트를 작성한다.

**완료 기준**:
- `Authorization: Bearer ...` 헤더가 있을 때/없을 때/형식이 다를 때 `resolveToken` 동작 검증
- `createAccessToken`으로 생성한 토큰을 `getAuthentication`으로 복원 시 username/roles/userId가 일치
- 만료된 토큰 검증 시 `ExpiredJwtCustomException` 발생
- 서명/형식이 유효하지 않은 토큰 검증 시 `InvalidJwtCustomException` 발생

**관련 규칙 위치**: `.claude/domain/SECURITY.md` "JwtTokenProvider (security.jwt)"

**대상 파일**: `src/test/java/com/lcs/lxp/security/jwt/JwtTokenProviderTest.java`(신규)

**진행 기록**: (착수 전)

---

## [SEC-02] JWT 인증 필터 단위 테스트

**설명**: `JwtAuthenticationFilter`의 정상 인증, 만료 토큰 시 재발급 시도(`tryRefresh`), 무효 토큰 시 컨텍스트 클리어 경로에 대한 단위 테스트를 작성한다.

**완료 기준**:
- 유효한 access token일 때 `SecurityContext`에 인증 정보가 설정됨
- access token 만료 + 유효한 refresh token일 때 재발급 후 `New-Access-Token` 응답 헤더 설정 및 새 인증 정보로 컨텍스트 갱신
- access token 만료 + refresh token 없음/재발급 실패(`InvalidRefreshTokenException`)일 때 `SecurityContext`가 비워짐
- access token이 무효(`InvalidJwtCustomException`)일 때 `SecurityContext`가 비워짐
- 모든 경우 `filterChain.doFilter`가 호출됨

**관련 규칙 위치**: `.claude/domain/SECURITY.md` "JwtAuthenticationFilter (security.jwt)"

**대상 파일**: `src/test/java/com/lcs/lxp/security/jwt/JwtAuthenticationFilterTest.java`(신규)

**진행 기록**: (착수 전)

---

## [SEC-03] 리프레시 토큰 재발급 단위 테스트

**설명**: `RefreshService.refreshAccessToken()`의 정상 재발급, 만료/무효 refresh token, DB 미존재/DB상 만료 경로에 대한 단위 테스트를 작성한다. `RefreshToken` 엔티티의 `isExpired()` 판단 로직도 함께 검증한다.

**완료 기준**:
- 유효한 refresh token으로 재발급 성공 시 `CustomUserDetailsService` 조회 결과 기반으로 새 access token 반환
- refresh token이 만료된 JWT일 때 DB 레코드 삭제 + `InvalidRefreshTokenException` 발생
- refresh token이 무효한 JWT일 때 `InvalidRefreshTokenException` 발생(DB 조회 없이)
- DB에 refresh token 레코드가 없을 때 `InvalidRefreshTokenException` 발생
- DB 레코드는 있으나 `isExpired()`가 true일 때 레코드 삭제 + `InvalidRefreshTokenException` 발생
- `RefreshToken.isExpired()`가 만료 전/후 시각에 대해 정확히 true/false를 반환

**관련 규칙 위치**: `.claude/domain/SECURITY.md` "RefreshService (security.refresh)", "RefreshToken (security.refresh, JPA 엔티티)"

**대상 파일**: `src/test/java/com/lcs/lxp/security/refresh/RefreshServiceTest.java`(신규), `src/test/java/com/lcs/lxp/security/refresh/RefreshTokenTest.java`(신규)

**진행 기록**: (착수 전)

---

## [SEC-04] 인증 Principal/UserDetailsService 단위 테스트

**설명**: `CustomUserPrincipal`의 `UserDetails` 구현(특히 `isEnabled()`)과 `CustomUserDetailsService.loadUserByUsername()`의 정상/예외 경로에 대한 단위 테스트를 작성한다.

**완료 기준**:
- `isDeleted=false`일 때 `isEnabled()`가 true, `isDeleted=true`일 때 false
- 존재하는 이메일로 조회 시 `CustomUserPrincipal`이 회원의 id/email/password/`ROLE_{역할명}` 권한으로 생성됨
- 존재하지 않는 이메일로 조회 시 `UsernameNotFoundException` 발생

**관련 규칙 위치**: `.claude/domain/SECURITY.md` "CustomUserPrincipal (security.principal)", "CustomUserDetailsService (security.service)"

**대상 파일**: `src/test/java/com/lcs/lxp/security/principal/CustomUserPrincipalTest.java`(신규), `src/test/java/com/lcs/lxp/security/service/CustomUserDetailsServiceTest.java`(신규)

**진행 기록**: (착수 전)
