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
