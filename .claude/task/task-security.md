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

**진행 기록**:
- 4-1(테스트 작성): `JwtTokenProviderTest.java` 신규 작성. `resolveToken`(헤더 있음/없음/다른 스킴), `resolveRefreshToken`, `createAccessToken`↔`getAuthentication` 왕복(username/userId/roles), `validateToken` 정상/만료(`ExpiredJwtCustomException`)/형식오류·타키서명(`InvalidJwtCustomException`), `createRefreshToken` 케이스 포함.
- 4-2(구현): 기존 `JwtTokenProvider.java` 구현이 완료 기준 및 SECURITY.md 서술을 그대로 충족함을 확인. 구현 코드 변경 없음.
- 4-3(리뷰): PASS. 변경 범위가 테스트 파일 1개뿐임을 확인, 완료 기준 4항목 모두 실질 검증(mock 우회 없음), test-rules.md(BDD 네이밍/`@DisplayName`/verify·assert) 준수 확인.
- 4-4(테스트 실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과. 전체 커버리지 85%(목표 80% 충족), `JwtTokenProvider` 커버리지 98%.
- 완료 근거: 리뷰 PASS + 테스트/PMD 통과 + 사용자 확인(2026-07-14).

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

**진행 기록**:
- 4-1(테스트 작성): `JwtAuthenticationFilterTest.java` 신규 작성(테스트 8개). 유효 토큰 인증 설정, 만료+유효 refresh 재발급/헤더/컨텍스트 갱신, 만료+refresh 없음/`InvalidRefreshTokenException`/그 외 예외 시 컨텍스트 비움, 무효 토큰 시 컨텍스트 비움, 모든 경우 `filterChain.doFilter` 호출 케이스 포함.
- 4-2(구현): 기존 `JwtAuthenticationFilter.java` 구현이 완료 기준 및 SECURITY.md 서술을 그대로 충족함을 확인. 구현 코드 변경 없음.
- 4-3(리뷰): PASS. 변경 범위가 테스트 파일 1개뿐임을 확인, 완료 기준 5항목 모두 실질 검증, mock 시그니처가 실제 구현과 일치, test-rules.md 준수, `SecurityContextHolder` 오염 방지(`@BeforeEach`/`@AfterEach` clearContext) 확인.
- 4-4(테스트 실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과. 커버리지 87%(목표 80% 충족).
- 완료 근거: 리뷰 PASS + 테스트/PMD 통과 + 사용자 확인(2026-07-14).

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

**진행 기록**:
- 4-1(테스트 작성): `RefreshServiceTest.java`(5개), `RefreshTokenTest.java`(2개) 신규 작성. 유효 토큰 재발급 성공, 만료 JWT 삭제+예외, 무효 JWT DB조회없이 예외, DB 레코드 없음 예외, DB상 만료 삭제+예외, `isExpired()` 전/후 판정 케이스 포함.
- 4-2(구현): 기존 `RefreshService.java`/`RefreshToken.java` 구현이 완료 기준 및 SECURITY.md 서술을 그대로 충족함을 확인. 구현 코드 변경 없음.
- 4-3(리뷰): PASS. 변경 범위가 테스트 파일 2개뿐임을 확인, 완료 기준 6항목 모두 실질 검증. `RefreshTokenTest`가 순수 assert만 사용하는 점(test-rules.md 문구와 형식적 차이)은 PMD `UnitTestShouldIncludeAssert`의 실제 목적(assert 또는 verify 중 하나)을 assert로 충족하므로 blocker/major 아님으로 판정.
- 4-4(테스트 실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과(RefreshTokenTest의 순수 assert 기반 테스트도 PMD 규칙 통과 확인). 커버리지 89%(목표 80% 충족).
- 완료 근거: 리뷰 PASS + 테스트/PMD 통과 + 사용자 확인(2026-07-14).

---

## [SEC-04] 인증 Principal/UserDetailsService 단위 테스트

**설명**: `CustomUserPrincipal`의 `UserDetails` 구현(특히 `isEnabled()`)과 `CustomUserDetailsService.loadUserByUsername()`의 정상/예외 경로에 대한 단위 테스트를 작성한다.

**완료 기준**:
- `isDeleted=false`일 때 `isEnabled()`가 true, `isDeleted=true`일 때 false
- 존재하는 이메일로 조회 시 `CustomUserPrincipal`이 회원의 id/email/password/`ROLE_{역할명}` 권한으로 생성됨
- 존재하지 않는 이메일로 조회 시 `UsernameNotFoundException` 발생

**관련 규칙 위치**: `.claude/domain/SECURITY.md` "CustomUserPrincipal (security.principal)", "CustomUserDetailsService (security.service)"

**대상 파일**: `src/test/java/com/lcs/lxp/security/principal/CustomUserPrincipalTest.java`(신규), `src/test/java/com/lcs/lxp/security/service/CustomUserDetailsServiceTest.java`(신규)

**진행 기록**:
- 4-1(테스트 작성): `CustomUserPrincipalTest.java`, `CustomUserDetailsServiceTest.java` 신규 작성. `isDeleted` 값에 따른 `isEnabled()` true/false, 존재 이메일(일반회원/강사) 조회 시 id/email/password/`ROLE_{역할명}`으로 principal 생성, 미존재 이메일 조회 시 `UsernameNotFoundException` 케이스 포함.
- 4-2(구현): 기존 `CustomUserPrincipal.java`/`CustomUserDetailsService.java` 구현이 완료 기준 및 SECURITY.md 서술을 그대로 충족함을 확인. 구현 코드 변경 없음.
- 4-3(리뷰): PASS. 변경 범위가 테스트 파일 2개뿐임을 확인, 완료 기준 3항목 모두 실질 검증. `Member`의 `id` 필드를 `ReflectionTestUtils.setField`로 주입하는 방식이 기존 `MemberServiceTest` 관례와 일치함을 확인.
- 4-4(테스트 실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과. 전체 커버리지 91.18%, security 패키지 커버리지 98.06%(목표 80% 대폭 상회) — SEC-01~04 완결로 Security 경량 BC 커버리지 보강 작업 종료.
- 완료 근거: 리뷰 PASS + 테스트/PMD 통과 + 사용자 확인(2026-07-14).
