# SECURITY.md

## 문서 성격

- `com.lcs.lxp.security` 패키지는 `.claude/ARCHITECTURE.md` 기준 Bounded Context가 아니라 `config`/`common`과 동급의 **공통 인프라** 패키지이다.
- 이 문서는 새 비즈니스 규칙을 정의하지 않는다. 이미 구현되어 있는 컴포넌트의 책임을 요약해, 테스트 커버리지 보강 작업(`SEC-01`~`SEC-04`)의 근거로만 사용한다.
- 동작 변경이 필요해지면 이 문서를 먼저 갱신하고 사용자 승인을 받은 뒤 진행한다(`.claude/LOOP.md` 3단계 절차 준용).

## 컴포넌트별 책임

### JwtTokenProvider (`security.jwt`)

- Authorization 헤더에서 access token 추출(`resolveToken`), `X-Refresh-Token` 헤더에서 refresh token 추출(`resolveRefreshToken`).
- JWT 서명 검증 및 파싱. 만료 시 `ExpiredJwtCustomException`, 그 외 파싱/서명 오류 시 `InvalidJwtCustomException` 발생(`validateToken`, 내부 `parseClaims`).
- `Authentication`으로부터 access token 생성(`createAccessToken`, subject/userId/roles claim 포함), refresh token 생성(`createRefreshToken`, claim 없이 만료시간만 포함).
- 토큰의 subject/roles/userId claim으로부터 `Authentication` 객체 재구성(`getAuthentication`).

### JwtAuthenticationFilter (`security.jwt`)

- `OncePerRequestFilter`로 매 요청마다 access token을 검증해 `SecurityContext`에 인증 정보를 설정한다.
- 토큰 만료(`ExpiredJwtCustomException`) 시 refresh token으로 재발급을 시도한다(`tryRefresh`). 성공 시 응답 헤더 `New-Access-Token`을 설정하고, refresh token이 없거나 재발급이 실패(`InvalidRefreshTokenException`, 그 외 예외)하면 `SecurityContext`를 비운다.
- 유효하지 않은 access token(`InvalidJwtCustomException`) 시 `SecurityContext`를 비운다.

### RefreshService (`security.refresh`)

- refresh token 값으로 access token을 재발급한다(`refreshAccessToken`).
- refresh token이 만료된 JWT(`ExpiredJwtCustomException`)면 DB에서 해당 레코드를 삭제한 뒤 `InvalidRefreshTokenException`을 던진다. JWT 자체가 유효하지 않으면(`InvalidJwtCustomException`) 즉시 `InvalidRefreshTokenException`을 던진다.
- DB에 저장된 `RefreshToken` 엔티티를 조회하지 못하거나(`Optional` 비어있음) 이미 만료(`isExpired()`)됐으면 `InvalidRefreshTokenException`을 던진다(만료된 경우 삭제 후 던짐).
- `CustomUserDetailsService`로 사용자를 조회해 새 access token을 발급한다.

### RefreshToken (`security.refresh`, JPA 엔티티)

- `email`/`token`/`expiryDate`를 저장한다.
- `isExpired()`는 현재 시각이 `expiryDate`를 지났는지로 판단한다.

### CustomUserPrincipal (`security.principal`)

- Spring Security `UserDetails` 구현체. `userId`/`email`/`authorities`/`isDeleted` 보유.
- `isEnabled()`는 `isDeleted`의 반대값을 반환한다(탈퇴 회원은 인증 비활성).

### CustomUserDetailsService (`security.service`)

- `UserDetailsService` 구현체. 이메일로 `Member`를 조회해 `CustomUserPrincipal`을 생성한다. 권한은 `ROLE_{회원 역할명}` 단일 권한이다.
- 회원을 찾지 못하면 `UsernameNotFoundException`을 던진다.

### 예외 (`security.exception`)

- `ExpiredJwtCustomException`: JWT 만료.
- `InvalidJwtCustomException`: JWT 서명/형식 오류.
- `InvalidRefreshTokenException`: refresh token 유효성 오류(만료/미존재/DB 미일치).

## 의존 관계

- `CustomUserDetailsService`가 Member 도메인(`Member`, `MemberRepository`)을 참조한다. 그 외 다른 BC에 대한 의존은 없다.
- 모든 BC의 인증/인가 요청 처리에 횡단으로 사용되지만, `TASK.md` BC 로드맵의 의존성 순서(계획 스케줄링용)에는 포함하지 않는다(경량 인프라 문서이므로).
