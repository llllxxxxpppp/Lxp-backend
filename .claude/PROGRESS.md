# 구현 완료 목록

## 강좌 컨텐츠 관리 바운디드 컨텍스트

### 강좌·강의·미션 도메인 모델 (2026-06-22)

**구현 내용**
- `common/exception/DomainException` — 도메인 예외 기반 추상 클래스
- `course/exception/CourseException` — 강좌 도메인 예외
- `course/model/ContentStatus` — 공개(PUBLIC) / 비공개(PRIVATE) 상태 enum
- `course/model/Title` — 제목 VO (@Embeddable), 빈 값·100자 초과 불허
- `course/model/InstructorId`, `CourseId`, `LectureId`, `MissionId` — ID VO (record)
- `course/model/Course` — 강좌 애그리거트 루트
- `course/model/Lecture` — 강의 엔티티
- `course/model/Mission` — 미션 엔티티
- `course/repository/CourseRepository`, `LectureRepository`, `MissionRepository`

**핵심 불변식**
- 강좌·강의·미션은 비공개 상태로 생성됨
- 제목은 비어있을 수 없고 100자를 초과할 수 없음
- 강좌 공개 조건: 활성화된 강의·미션 각각 1개 이상 필요
- 공개 상태에서 강좌 수정·강의/미션 추가 불가
- 강좌가 비공개면 강의·미션의 상태와 무관하게 수정 허용
- 강좌가 공개면 강의·미션은 자신의 상태에 따라 수정 가능 여부 결정
- 비공개(unpublish) 처리는 soft delete와 동일 (deleted = true)

**테스트 현황**: TitleTest(6), CourseTest(12), LectureTest(8), MissionTest(8) — 전체 통과

### 강좌 컨트롤러 요청 검증 (2026-06-23)

**구현 내용**
- Request DTO에 Bean Validation 어노테이션 추가 (`@NotBlank`, `@Size`)
  - `CreateCourseRequest`: `title`(@NotBlank, max=100), `description`(@NotBlank, max=4096)
  - `UpdateCourseRequest`: `title`(@NotBlank, max=100), `description`(@NotBlank, max=4096)
  - `AddLectureRequest`: `title`(@NotBlank, max=100), `contentUrl`(@NotBlank)
  - `AddMissionRequest`: `title`(@NotBlank, max=100), `content`(@NotBlank, max=4096)
- `CourseController` 메서드 파라미터에 `@Valid` 추가 (createCourse, updateCourse, addLecture, addMission)
- `CourseExceptionHandler`에 `MethodArgumentNotValidException` 핸들러 추가 → 첫 번째 필드 오류 메시지로 400 반환

**테스트 현황**: CourseControllerTest(28) — 전체 통과 (기존 12 + 검증 실패 16 추가)

---

### 강좌 목록 조회 (페이지네이션·검색) (2026-06-24)

**구현 내용**
- `course/dto/response/CoursePageResponse` — 페이지 응답 DTO (record, `from(Page<Course>)` 팩토리 메서드)
- `CourseRepository` — `findAllByStatus`, `findByStatusAndTitleKeyword` (JPQL, 제목 부분 일치·대소문자 무시) 쿼리 메서드 추가
- `CourseService.getCourses(keyword, page, size)` — 키워드 없으면 전체 PUBLIC 강좌, 있으면 제목 검색

**엔드포인트**
- `GET /api/courses` — 공개 강좌 목록 조회 (200): `keyword`(optional), `page`(default=0), `size`(default=10) 쿼리 파라미터

**응답 DTO**
- `CoursePageResponse` — courses(`List<CourseSummaryResponse>`), page, size, totalElements, totalPages, last

**테스트 현황**: CourseControllerTest(+3), CourseServiceTest(+4) — 전체 통과, PMD 통과

---

### 강좌 컨트롤러 (2026-06-23)

**구현 내용**
- `course/dto/request/CreateCourseRequest` — 강좌 생성 요청 DTO (record)
- `course/dto/request/UpdateCourseRequest` — 강좌 수정 요청 DTO (record)
- `course/dto/request/AddLectureRequest` — 강의 추가 요청 DTO (record)
- `course/dto/request/AddMissionRequest` — 미션 추가 요청 DTO (record)
- `course/dto/response/ErrorResponse` — 에러 응답 DTO (record)
- `course/controller/CourseController` — 강좌 REST 컨트롤러 (`@RestController`)
- `course/exception/CourseExceptionHandler` — `CourseException` → 400 Bad Request 매핑

**엔드포인트**
- `POST /api/courses` — 강좌 생성 (201), instructorId는 `Authentication.getName()` 에서 추출
- `PATCH /api/courses/{courseId}` — 강좌 제목 수정 (200)
- `POST /api/courses/{courseId}/publish` — 강좌 공개 (200)
- `POST /api/courses/{courseId}/unpublish` — 강좌 비공개 (200)
- `GET /api/courses/{courseId}` — 강좌 요약 조회 (200): 강좌 기본 정보만 반환 (강의·미션 제외)
- `GET /api/courses/{courseId}/detail` — 강좌 상세 조회 (200): 강의·미션 목록 포함
- `POST /api/courses/{courseId}/lectures` — 강의 추가 (201)
- `POST /api/courses/{courseId}/lectures/{lectureId}/publish` — 강의 공개 (200)
- `POST /api/courses/{courseId}/lectures/{lectureId}/unpublish` — 강의 비공개 (200)
- `POST /api/courses/{courseId}/missions` — 미션 추가 (201)
- `POST /api/courses/{courseId}/missions/{missionId}/publish` — 미션 공개 (200)
- `POST /api/courses/{courseId}/missions/{missionId}/unpublish` — 미션 비공개 (200)

**응답 DTO**
- `CourseSummaryResponse` — courseId, instructorId, title, status
- `CourseDetailResponse` — CourseSummaryResponse + lectures(`LectureResponse`), missions(`MissionResponse`)
- `LectureResponse` — lectureId, title, status
- `MissionResponse` — missionId, title, status

**테스트 현황**: CourseControllerTest(12) — 전체 통과

### 강좌 서비스 (2026-06-22)

**구현 내용**
- `member/model/MemberRole` — 회원 유형 enum (MEMBER, INSTRUCTOR, ADMIN)
- `course/service/CourseService` — 강좌 애플리케이션 서비스

**핵심 역할**
- `createCourse`: 강사만 강좌를 생성할 수 있음, 생성 후 저장
- `updateCourse`: 강좌 제목 수정 (공개 상태 체크는 도메인 위임)
- `publishCourse` / `unpublishCourse`: 강사 또는 어드민 권한 검증 후 위임
- `addLecture` / `publishLecture` / `unpublishLecture`: 강의 추가·공개·비공개
- `addMission` / `publishMission` / `unpublishMission`: 미션 추가·공개·비공개
- 역할 조건 불충족 또는 강좌 미존재 시 `CourseException` 발생

**테스트 현황**: CourseServiceTest(43) — 전체 통과

### 강의·미션 물리 삭제 엔드포인트 제거 (2026-06-23)

Lecture, Mission 엔티티는 물리적으로 삭제되어선 안 되므로 잘못 추가된 삭제 메서드를 제거함.

**제거된 항목**
- `DELETE /api/courses/{courseId}/lectures/{lectureId}` — 강의 삭제 엔드포인트
- `DELETE /api/courses/{courseId}/missions/{missionId}` — 미션 삭제 엔드포인트
- `CourseService.removeLecture(Long, Long)` — 강의 삭제 서비스 메서드
- `CourseService.removeMission(Long, Long)` — 미션 삭제 서비스 메서드
- `CourseServiceTest` 내 removeLecture·removeMission 관련 테스트 10개

---

### 강좌 권한 검증 로직을 Spring Security로 이관 (2026-07-02)

`CourseService`가 `SecurityContextHolder`를 직접 조회해 역할을 검증하던 방식(`requireRole`)을 제거하고, 동일한 접근 제어 규칙을 `SecurityConfig`의 `requestMatchers()` 기반 URL 규칙으로 이관함. 서비스 레이어는 인가 책임 없이 순수 비즈니스 로직만 담당하도록 정리.

**구현 내용**
- `CourseService` — `requireRole(MemberRole...)` 메서드 및 모든 호출부 삭제, `MemberRole`/`Authentication`/`SecurityContextHolder` import 제거
- `SecurityConfig.securityFilterChain` — `authorizeHttpRequests()`에 `requestMatchers(HttpMethod, path)` 규칙 9개 추가 (기존 `requireRole` 호출 지점과 1:1 대응)
  - `POST /api/courses`, `.../publish`, `.../lectures`, `.../lectures/{lectureId}/publish`, `.../missions`, `.../missions/{missionId}/publish` → `hasRole("INSTRUCTOR")`
  - `POST /api/courses/{courseId}/unpublish`, `.../lectures/{lectureId}/unpublish`, `.../missions/{missionId}/unpublish` → `hasAnyRole("INSTRUCTOR", "ADMIN")`
  - 그 외 GET 엔드포인트·`updateCourse`는 기존대로 역할 제한 없음(인증만 필요) 유지

**핵심 변경 사항**
- 인가 실패 시 응답이 기존 400(`CourseException`, 서비스 레이어에서 발생)에서 403 Forbidden(`CustomAccessDeniedHandler`)으로 변경됨. 미인증 요청은 401(`CustomAuthenticationEntryPoint`)
- 요청이 컨트롤러/서비스 레이어에 도달하기 전에 Security 필터 단계에서 차단되므로, 인가 여부에 따른 리소스 접근을 원천 차단

**테스트 현황**
- `CourseServiceTest`: 역할 기반 인가 실패를 검증하던 19개 테스트 제거 (책임이 Security 레이어로 이동), 나머지 테스트는 인증 절차 없이 동작하도록 정리 — 28개 전체 통과
- `SecurityConfigTest`(신규) — `@SpringBootTest` + 실제 `SecurityConfig`를 로드하는 통합 테스트로 9개 보호 엔드포인트에 대한 역할별 200/201/403/401 응답과 `verify()`/`verifyNoInteractions()`를 통한 서비스 도달 여부 검증 — 29개 전체 통과
- PMD(`pmdMain`, `pmdTest`) 통과

---

## 회원 관리 바운디드 컨텍스트

### 회원 도메인 모델 (2026-06-24)

**구현 내용**
- `member/exception/MemberException` — 회원 도메인 예외
- `member/model/MemberRole` — 회원 유형 enum (MEMBER, INSTRUCTOR, ADMIN)
- `member/model/vo/MemberId` — 회원 ID VO (record)
- `member/model/vo/InstructorProfile` — 강사 프로필 VO (@Embeddable, 이름·프로필사진·자기소개)
- `member/model/entity/Member` — abstract 애그리거트 루트 (`@Inheritance(SINGLE_TABLE)`, discriminator column: `role`)
- `member/model/entity/RegularMember` — 일반 회원 서브클래스 (`withdraw`)
- `member/model/entity/InstructorMember` — 강사 서브클래스 (`InstructorProfile` 소유, `updateProfile`, `suspend`)
- `member/model/entity/AdminMember` — 어드민 서브클래스

**핵심 불변식**
- 회원 유형: MEMBER (일반 회원), INSTRUCTOR (강사), ADMIN (어드민)
- 이메일은 비어있을 수 없고, 형식이 유효해야 함
- 패스워드는 비어있을 수 없음 (인코딩은 서비스 레이어 책임)
- 강사 프로필 이름은 비어있을 수 없음
- 프로필 사진·자기소개는 선택 값 (null 허용)
- 수정 일자는 생성 시 null, 수정 시에만 갱신
- 탈퇴(`withdraw`) / 강사 정지(`suspend`): soft delete (deleted = true)
- 프로필 수정은 `InstructorMember` 타입만 가능 (컴파일 타임 타입 안전성)

**팩토리 메서드**
- `RegularMember.create(email, encodedPassword)` — 일반 회원 생성
- `InstructorMember.create(email, encodedPassword, name, profileImageUrl, introduction)` — 강사 생성
- `AdminMember.create(email, encodedPassword)` — 어드민 생성

**테스트 현황**: InstructorProfileTest(6), MemberTest(24) — 전체 통과, PMD 통과

### 회원 서비스·컨트롤러 (2026-06-24)

**구현 내용**
- `member/dto/request/LoginRequestDTO` — 로그인 요청 DTO (record, @NotBlank)
- `member/dto/request/SignupRequestDTO` — 회원가입 요청 DTO (record, @NotBlank)
- `member/dto/response/TokenResponseDTO` — AccessToken·RefreshToken 응답 DTO
- `member/dto/response/UserResponseDTO` — 회원 정보 응답 DTO (from 팩토리 메서드)
- `member/repository/MemberRepository` — JPA Repository (`existsByEmail`, `findByEmail`)
- `member/service/MemberService` — 로그인·회원가입·로그아웃 서비스
- `member/controller/MemberController` — 회원 인증 REST 컨트롤러 (`@RestController`)

**엔드포인트**
- `POST /api/auth/login` — 로그인 (200): 이메일·패스워드 인증 후 AccessToken + RefreshToken 반환
- `POST /api/auth/signup` — 회원가입 (201): 이메일 중복 검증 후 일반 회원(`RegularMember`) 생성
- `POST /api/auth/logout` — 로그아웃 (204): `X-Refresh-Token` 헤더로 DB에서 Refresh Token 삭제

**핵심 불변식**
- 이미 사용 중인 이메일로 가입 시 `MemberException` 발생
- 비밀번호 인코딩은 서비스 레이어 책임 (`BCryptPasswordEncoder`)
- 로그인 시 기존 Refresh Token이 있으면 교체(rotate)

### JWT 기반 Spring Security 인증 (2026-06-24)

**구현 내용**
- `security/config/SecurityConfig` — Spring Security 설정 (Stateless 세션, CORS, JWT 필터 등록, H2 콘솔 접근)
- `security/jwt/JwtTokenProvider` — JWT 생성·검증·파싱 (HS512, AccessToken·RefreshToken 분리)
- `security/jwt/JwtAuthenticationFilter` — `OncePerRequestFilter` (AccessToken 검증, 만료 시 RefreshToken으로 자동 재발급)
- `security/refresh/RefreshToken` — Refresh Token 엔티티 (email, token, expiresAt)
- `security/refresh/RefreshTokenRepository` — JPA Repository (`findByEmail`, `findByToken`)
- `security/refresh/RefreshService` — RefreshToken 검증 후 AccessToken 재발급 서비스
- `security/service/CustomUserDetailsService` — Spring Security `UserDetailsService` 구현체
- `security/principal/CustomUserPrincipal` — `UserDetails` 구현체 (userId, roles 포함)
- `security/exception/ExpiredJwtCustomException` — 만료된 JWT 예외
- `security/exception/InvalidJwtCustomException` — 유효하지 않은 JWT 예외
- `security/exception/InvalidRefreshTokenException` — 유효하지 않은 Refresh Token 예외
- `security/handler/CustomAccessDeniedHandler` — 권한 없음 (403) 핸들러
- `security/handler/CustomAuthenticationEntryPoint` — 미인증 (401) 핸들러

**접근 제어 정책**
- `/api/auth/**` — 인증 없이 접근 허용
- `/api/admin/**` — `ADMIN` 권한 필요
- `/api/member/**` — `MEMBER` 권한 필요
- 그 외 — 인증 필요

**핵심 불변식**
- AccessToken 만료 시 `X-Refresh-Token` 헤더의 토큰으로 자동 재발급, 응답 헤더 `New-Access-Token`에 반환
- Refresh Token 만료·DB 미존재 시 재발급 거부, SecurityContext 초기화
- H2 콘솔 접근은 `spring.h2.console.enabled=true` 일 때 Security 우회 (`@ConditionalOnProperty`)

**부수 수정**
- `CourseControllerTest`, `SubscriptionControllerTest` — Spring Security 적용에 따른 `@MockitoBean` 추가 (JwtTokenProvider, RefreshService)

---

### 정지(suspend)/탈퇴(withdraw) 상태 모델 분리 (2026-07-07)

`MEMBER.md` 갱신에 따라 REPORT.md에서 발견된 불일치(정지와 탈퇴가 `deleted` 단일 플래그로 뭉뚱그려져 있던 문제)를 해소함.

**구현 내용**
- `member/model/entity/Member` — `suspendedAt`(`OffsetDateTime`, nullable) 필드, `getSuspendedAt()`, `isSuspended()`, `protected markSuspended()` 추가
- `member/model/entity/RegularMember` — `withdrawnAt`(`OffsetDateTime`, nullable) 필드, `getWithdrawnAt()` 추가. `withdraw()`는 `markDeleted()` + `withdrawnAt` 설정. `suspend()` 신규 추가 — `markSuspended()` + `markDeleted()` 모두 호출
- `member/model/entity/InstructorMember` — `suspend()`가 `markSuspended()`와 `markDeleted()`를 모두 호출하도록 변경 (기존엔 `markDeleted()`만 호출)

**핵심 불변식**
- 정지 일시(`suspendedAt`)와 탈퇴 일시(`withdrawnAt`)는 서로 독립적으로 관리되며, 둘 다 NULL이면 각각 미정지·미탈퇴 상태
- 일반 회원·강사 모두 정지(`suspend()`) 시 `deleted=true`로 설정됨 — 정지된 회원은 로그인이 차단되어야 하므로(`CustomUserPrincipal.isEnabled() = !isDeleted`), 정지도 탈퇴와 동일하게 soft delete 플래그를 공유함. 정지와 탈퇴의 구분은 `suspendedAt`/`withdrawnAt` 타임스탬프로 판별
- 강사 정지: `deleted=true` + `suspendedAt` 설정 (기존과 동일하게 soft delete)
- 일반 회원 탈퇴: `deleted=true` + `withdrawnAt` 설정, `suspendedAt`은 그대로 유지(null이면 정지 이력 없음)
- 일반 회원 정지: `deleted=true` + `suspendedAt` 설정, `withdrawnAt`은 그대로 유지(null이면 탈퇴 이력 없음)

**테스트 현황**: `MemberTest`(29) — 전체 통과, PMD 통과, 전체 회귀 테스트 통과

**후속 구현 필요 (이번 라운드에서 미구현, 순서대로)**
1. 구독권 관리 바운디드 컨텍스트에 `MemberSuspendedEvent`/`MemberWithdrawnEvent` 리스너 추가 후 기존 `SubscriptionService.suspendSubscription(memberId)`와 연결(정지 시), 탈퇴 시 구독권 추가 처리 로직 신규 구현
2. 강좌 컨텐츠 관리 바운디드 컨텍스트에 `InstructorSuspendedEvent` 리스너 추가 + 해당 강사의 강좌를 비공개 처리하는 기능 신규 구현 (리스너는 ARCHITECTURE.md 규칙에 따라 처리 시작/종료를 info 레벨로 로그해야 함)
3. 구독권 관리 바운디드 컨텍스트에 "회원이 유효한 구독권을 보유 중인지" 조회하는 공개 메서드 추가 (회원 신고 기능 재개 시 필요, 이번 라운드에서는 회원 신고 기능 자체를 사용자 결정에 따라 구현하지 않음)
4. 회원 이메일 자기수정 기능은 사용자 결정에 따라 구현하지 않기로 함 (`Member.updateEmail()` 도메인 메서드는 존재하나 호출 지점 없음, 의도적 상태)

---

### 도메인 이벤트 인프라 + 정지/탈퇴 이벤트 발행 (2026-07-08)

`MEMBER.md`에 이벤트 규칙(회원 정지 이벤트, 회원 탈퇴 이벤트, 강사 정지 이벤트)이 명시되어, 프로젝트 최초로 도메인 이벤트 인프라를 구현하고 회원 관리 컨텍스트에서 이벤트를 발행하도록 함.

**구현 내용**
- `common/event/BaseDomainEvent` (신규, 전역 공통) — `eventId`(`UUID`, `UUID.randomUUID()`로 생성, 불변), `occurredAt`(`OffsetDateTime`, 생성 시점 고정, 불변) 보유
- `common/event/DomainEventLogger` (신규, 전역 공통) — `@EventListener(BaseDomainEvent.class)`로 모든 도메인 이벤트 발행을 info 레벨로 로그 (PMD `GuardLogStatement` 준수를 위해 `isInfoEnabled()` 가드 적용)
- `member/event/MemberSuspendedEvent` — `BaseDomainEvent` 상속, `memberId` 필드
- `member/event/MemberWithdrawnEvent` — `BaseDomainEvent` 상속, `memberId` 필드
- `member/event/InstructorSuspendedEvent` — `BaseDomainEvent` 상속, `instructorId` 필드
- `member/service/MemberService` — `ApplicationEventPublisher` 의존성 추가, `suspendMember(Long)`/`withdrawMember(Long)`/`suspendInstructor(Long)` 신규 추가. 각각 대상 조회(없으면 예외) → 타입 검증(`RegularMember`/`InstructorMember`가 아니면 예외) → 도메인 상태 변경(`suspend()`/`withdraw()`) → 저장 → 해당 이벤트 발행 순서로 동작

**핵심 불변식**
- `BaseDomainEvent`는 PMD `AbstractClassWithoutAbstractMethod` 위반을 피하기 위해 `abstract`가 아닌 일반 클래스로 구현 (protected 생성자로 직접 인스턴스화는 방지)
- `suspendMember`/`withdrawMember`는 대상이 `RegularMember`가 아니면 예외, `suspendInstructor`는 대상이 `InstructorMember`가 아니면 예외
- 구독권/강좌 바운디드 컨텍스트 쪽 이벤트 리스너(실제 정지/비공개 처리)는 이번 범위에서 구현하지 않음 — 위 "후속 구현 필요" 항목 참고
- `withdrawMember`를 호출하는 자기수정 API(회원 본인 탈퇴)는 아직 없음 — STEP4(회원 자기수정 API) 작업에서 함께 추가 예정

**테스트 현황**: `MemberServiceTest`(신규, 9개: suspendMember 3 + withdrawMember 3 + suspendInstructor 3) — 전체 통과, PMD 통과, 전체 회귀 테스트 통과

---

### 어드민 전용 강사 생성/정지 API (2026-07-08)

`MEMBER.md` 어드민 섹션("강사 유형의 회원은 어드민만 생성 가능하다.", "어드민만 강사를 정지할 수 있다.")을 반영. REPORT.md 5번 항목(어드민 전용 강사 생성/정지 권한 검증 지점 없음) 해소.

**구현 내용**
- `member/dto/request/RegisterInstructorRequest` (신규) — email/password/name `@NotBlank`, profileImageUrl/introduction은 nullable
- `member/service/MemberService.registerInstructor(...)` (신규) — 기존 `register()`와 동일 패턴(이메일 중복 검사 → `InstructorMember.create` → 저장 → `UserResponseDTO` 반환)
- `member/controller/AdminMemberController` (신규) — `POST /api/admin/members/instructors`(강사 생성, 201), `POST /api/admin/members/instructors/{instructorId}/suspend`(강사 정지, 200, 기존 `suspendInstructor` 호출)
- `security/config/SecurityConfig` — **버그 수정**: `/api/admin/**`·`/api/member/**` 규칙이 `hasAnyAuthority("ADMIN"/"MEMBER")`로 되어 있었는데, 실제 부여되는 권한 문자열(`CustomUserDetailsService`가 생성하는 `"ROLE_" + role.name()`)과 불일치해 해당 경로가 항상 403이었던 기존 버그를 발견. `hasRole("ADMIN")`/`hasRole("MEMBER")`로 수정 (기존 `/api/courses/**`의 `hasRole("INSTRUCTOR")` 패턴과 통일)

**핵심 불변식**
- 강사 생성·정지 엔드포인트는 `/api/admin/**` 경로 규칙에 의해 ADMIN 권한만 접근 가능 (서비스 레이어에는 권한 검증 로직 없음 — 기존 `CourseService` 리팩토링과 동일하게 인가는 SecurityConfig가 전담)

**테스트 현황**: `MemberServiceTest`(+2, registerInstructor), `AdminMemberControllerTest`(신규, 7개: ADMIN/MEMBER/INSTRUCTOR/미인증 권한별 응답 검증) — 전체 통과, PMD 통과, 전체 회귀 테스트 통과 (기존 `SecurityConfigTest`의 course 관련 테스트도 영향 없음 확인)

---

### 회원 자기수정 API 노출 (2026-07-08)

`MEMBER.md`의 "회원 스스로 패스워드를 수정할 수 있다"(공통), "강사 프로필은 강사 본인이 수정할 수 있다"(강사) 규칙을 반영. 기존에 도메인 메서드(`updatePassword`/`updateProfile`)만 존재하고 호출 지점이 없었던 REPORT.md 6번 항목(비밀번호·강사 프로필 부분)을 해소. 회원 본인 탈퇴(`withdrawMember`, STEP2에서 구현)도 이번에 처음으로 API로 노출됨.

**구현 내용**
- `member/dto/request/ChangePasswordRequest`, `UpdateInstructorProfileRequest` (신규)
- `member/service/MemberService.changePassword(Long, String, String)` (신규) — 현재 비밀번호를 `PasswordEncoder.matches`로 검증 후 `updatePassword`
- `member/service/MemberService.updateInstructorProfile(Long, String, String, String)` (신규) — 대상이 `InstructorMember`가 아니면 예외, 맞으면 `updateProfile` 호출 후 `UserResponseDTO` 반환
- `member/controller/MemberSelfController` (신규, `/api/members/me`) — `PATCH /password`(204), `PATCH /instructor-profile`(200 + `UserResponseDTO`), `DELETE /`(204, 기존 `withdrawMember` 재사용)
- 인증된 사용자 식별은 기존 `CourseController` 패턴(`Long.parseLong(authentication.getName())`) 재사용, `SecurityConfig` 수정 없이 기본 `anyRequest().authenticated()` 규칙으로 보호됨

**핵심 불변식**
- 이메일 자기수정은 사용자 결정에 따라 이번 범위에서 제외 (`Member.updateEmail()` 도메인 메서드는 여전히 호출 지점 없음, 의도적 상태로 유지)

**테스트 현황**: `MemberServiceTest`(+6: changePassword 3 + updateInstructorProfile 3, 누적 17개), `MemberSelfControllerTest`(신규, 6개) — 전체 통과, PMD 통과, 전체 회귀 테스트 통과

---

## 구독권 결제 관리 바운디드 컨텍스트

### 구독권·결제 도메인 모델 + 서비스 + 컨트롤러 (2026-06-23)

**구현 내용 (헥사고날 아키텍처)**
- `subscription/domain/exception/SubscriptionException` — 구독 도메인 예외
- `subscription/domain/model/vo/SubscriptionId`, `SubscriptionStatus` — 구독권 ID VO, 상태 enum
- `subscription/domain/model/vo/PaymentId`, `PaymentStatus`, `PaymentInfo` — 결제 ID VO, 상태 enum, 요청 정보 @Embeddable
- `subscription/domain/model/vo/PaymentSuccessResponse`, `PaymentFailureResponse` — 결제 결과 VO
- `subscription/domain/model/vo/RefundInfo`, `RefundSuccessResponse`, `RefundFailureResponse` — 환불 VO
- `subscription/domain/model/entity/Subscription` — 구독권 애그리거트 루트
- `subscription/domain/model/entity/Payment` — 결제 애그리거트
- `subscription/domain/repository/SubscriptionRepository`, `PaymentRepository`
- `subscription/infrastructure/PaymentGateway` — 외부 결제 대행사 포트 인터페이스
- `subscription/infrastructure/DummyPaymentGateway` — 더미 구현체 (항상 성공)
- `subscription/infrastructure/PaymentAdapter` — 결제 흐름 조율 (requestPayment, requestRefund)
- `subscription/infrastructure/PaymentResult` — 결제 결과 record
- `subscription/application/dto/response/SubscriptionResponse`
- `subscription/application/service/SubscriptionService` — 애플리케이션 서비스
- `subscription/presentation/SubscriptionController` — REST 컨트롤러
- `subscription/presentation/SubscriptionExceptionHandler` — 예외 핸들러

**엔드포인트**
- `POST /api/subscriptions` — 구독권 생성 (201): 첫 구독 무료, 이후 유료(9900원), 더미 결제 동기 처리
- `GET /api/subscriptions/{id}` — 구독권 조회 (200)
- `POST /api/subscriptions/{id}/cancel` — 구독권 취소 (200): 유료·14일 이내 시 자동 환불
- `POST /api/subscriptions/reissue` — 만료 임박 구독권 재발급 (200): 관리자용 수동 트리거

**핵심 불변식**
- 구독권은 INACTIVE로 생성, 무료면 즉시 ACTIVE, 유료면 더미 결제 후 ACTIVE
- 유효기간: 31일 / 환불 기간: 활성화 후 14일
- 재발급: 기존 DISCARDED + 신규 생성 + 결제
- 회원 정지 시 구독권 SUSPENDED (`suspendSubscription(memberId)` 메서드 제공)

**부수 수정**
- `course/model/entity/Mission.java` — 기존 PMD 위반(`4096` 매직 넘버) 상수로 수정

**테스트 현황**: PaymentTest(15), SubscriptionTest(18), SubscriptionServiceTest(12), SubscriptionControllerTest(8) — 전체 통과, PMD 통과
