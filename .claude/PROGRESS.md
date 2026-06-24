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
