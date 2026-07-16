# TASK.md

`.claude/LOOP.md`에 따른 작업 상태의 원본(source of truth) 문서이다.

## BC 로드맵 (확정: 2026-07-13)

### 문서 참조 정책

- `.claude/domain/*_temp.md` 형태의 모든 파일(예: `MEMBER_temp.md`, `COURSE_temp.md`)은 도메인 정의 참조에서 **전부 제외**한다. 정식 도메인 정의는 `.claude/domain/[BC명].md` 본 문서만을 근거로 한다.

| BC | 핵심 책임 | 의존 방향 (← 의존함) | 도메인 정의 완성 여부 |
|---|---|---|---|
| 회원 관리 (Member) | 회원가입/인증, 강사, 어드민 | 없음 (base BC) | ✅ 완료 |
| 구독권 결제 관리 (Subscription) | 구독권, 결제 | Member | ✅ 완료 |
| 강좌 컨텐츠 관리 (Course) | 강좌/강의/미션 | Member | ✅ 완료 (2026-07-13 Status VO 열거형 유지로 확정) |
| 미션 답안 관리 (MissionAnswer) | 미션답안/댓글 | Member, Course, Subscription | ✅ 완료 |
| 강좌 후기 관리 (CourseReview) | 강좌 후기 | Member, Course, Subscription | ✅ 완료 |
| Security (공통 인프라, 경량 관리용) | JWT 인증/인가, 리프레시 토큰, UserDetailsService | 없음 (계획 스케줄링 순서에 미포함, 2026-07-13 사용자 확정) | ✅ 완료 (경량, `.claude/domain/SECURITY.md`) |

**의존성 순서 (선행 → 후행):** Member → Subscription, Course → MissionAnswer → CourseReview
(Security는 `.claude/ARCHITECTURE.md` 기준 Bounded Context가 아닌 공통 인프라이므로 이 의존성 순서에는 포함하지 않는다. 기존에 이미 구현된 코드의 테스트 커버리지 보강 목적으로만 경량 관리한다.)

### Member 도메인 관련 확정 사항 (2026-07-13, 사용자 확정)

- `.claude/domain/MEMBER_temp.md`는 참조에서 제외한다. 정식 도메인 정의는 `.claude/domain/MEMBER.md`만을 근거로 한다.
  - 근거: `MEMBER.md`는 최초 작성(6a4392d) 시점부터 현재까지 "강사 유형의 회원은 어드민만 생성 가능하다"를 일관되게 유지하고 있어 자체 모순이 없음. 모순은 `MEMBER_temp.md`(별도 메모)에만 존재했음.
- 강사 자진 탈퇴 기능은 이번 범위에 **미포함** — 강사는 정지만 가능(현행 유지).
- 정지/탈퇴 회원 자료 2년 보존 정책은 이번 범위에 **미포함** — 추후 별도 진행.

## 작업 요약표

| ID | 상태 | 작업명 | 도메인 | 담당 | 의존성 |
|---|---|---|---|---|---|
| MEMBER-01 | 🟢 | 회원 도메인 모델 (SINGLE_TABLE 상속, 검증, 타임스탬프) | Member | - | 없음 |
| MEMBER-02 | 🟢 | 회원가입/로그인/로그아웃 (JWT) | Member | - | MEMBER-01 |
| MEMBER-03 | 🟢 | 어드민 전용 강사 생성/정지 API | Member | - | MEMBER-01, MEMBER-02 |
| MEMBER-04 | 🟢 | 회원 자기 관리 (비밀번호 변경/강사 프로필 수정/자진 탈퇴) | Member | - | MEMBER-01, MEMBER-02 |
| MEMBER-05 | 🟢 | 회원 정지 처리 + 이벤트 발행 | Member | - | MEMBER-01 |
| MEMBER-06 | 🟢 | 탈퇴 회원 이메일 마스킹 | Member | - | MEMBER-04 |
| MEMBER-07 | 🟢 | MemberSelfController principal instanceof 검사 추가 (크로스커팅 정합화) | Member | - | MEMBER-04 |

세부 내용은 `.claude/task/task-member.md` 참고.

| COURSE-01 | 🟢 | 강좌/강의/미션 도메인 모델 (VO·엔티티·초기 상태) | Course | - | 없음 |
| COURSE-02 | 🟢 | 강좌 생성/수정/조회 API | Course | - | COURSE-01 |
| COURSE-03 | 🟢 | 강좌 목록 페이지네이션·검색 | Course | - | COURSE-02 |
| COURSE-04 | 🟢 | 강의/미션 추가 및 공개·비공개 API | Course | - | COURSE-01 |
| COURSE-05 | 🟢 | 강의 자료 타입(확장자) 필드 도입 | Course | - | COURSE-01 |
| COURSE-06a | 🟢 | 강좌/강의/미션 soft delete 모델 | Course | - | COURSE-01 |
| COURSE-06b | 🟢 | 강좌/강의/미션 삭제 API | Course | - | COURSE-06a |
| COURSE-07a | 🟢 | Sortable 인터페이스 + 순번 필드 | Course | - | COURSE-01 |
| COURSE-07b | 🟢 | 강좌 단위 순서 변경 API | Course | - | COURSE-07a |
| COURSE-08a | 🟢 | InstructorSuspendedEvent 리스너(정지 강사 강좌 비공개) | Course | - | MEMBER-03 |
| COURSE-08b | 🟢 | 정지된 강사 2차 방어 인터셉터 | Course | - | COURSE-08a |
| COURSE-09 | 🟢 | 강좌/강의/미션 소유권(작성 강사 본인) 검증 전체 적용 | Course | - | 없음 |
| COURSE-10 | 🟢 | 강의/미션 순서 보장 강화 및 물리 삭제 제거 | Course | - | COURSE-06a, COURSE-07a, COURSE-07b |
| COURSE-11 | 🟢 | CourseController principal instanceof 검사 추가 (크로스커팅 정합화) | Course | - | COURSE-09 |

세부 내용은 `.claude/task/task-course.md` 참고.

| SUB-01 | 🟢 | 구독권 애그리거트 재설계 (상태 표현·재발급 체인·유효기간) | Subscription | - | 없음 |
| SUB-02 | 🟢 | Payment 애그리거트 요청 리스트 구조 재설계 | Subscription | - | SUB-01 |
| SUB-03 | 🟢 | 결제/환불 이벤트 기반 아키텍처 전환 | Subscription | - | SUB-02 |
| SUB-04 | 🟢 | 구독권 조회/취소 API 정합성 조정 (수동 생성 API 제거, 환불 조건 복원) | Subscription | - | SUB-01, SUB-02 |
| SUB-05 | 🟢 | 회원가입 이벤트 리스너(무료 구독권 자동 발급) | Subscription | - | SUB-01, MEMBER-02 |
| SUB-06 | 🟢 | 회원 정지/탈퇴 이벤트 리스너(구독권 정지·환불) | Subscription | - | SUB-01, SUB-04, MEMBER-05, MEMBER-04 |
| SUB-07 | 🟢 | 만료 임박 구독권 자동 재발급 배치 | Subscription | - | SUB-01, SUB-03 |
| SUB-08 | 🟢 | SubscriptionController 인증 사용자 ID 추출 버그 수정 (크로스커팅 정합화) | Subscription | - | SUB-04 |

세부 내용은 `.claude/task/task-subscription.md` 참고.

| SEC-01 | 🟢 | JWT 토큰 발급/파싱 단위 테스트 | Security | - | 없음 |
| SEC-02 | 🟢 | JWT 인증 필터 단위 테스트 | Security | - | 없음 |
| SEC-03 | 🟢 | 리프레시 토큰 재발급 단위 테스트 | Security | - | 없음 |
| SEC-04 | 🟢 | 인증 Principal/UserDetailsService 단위 테스트 | Security | - | 없음 |
| SEC-05 | 🔵 | Swagger(OpenAPI) API 명세서 열람 도입 | Security | 마스터 에이전트 세션(2026-07-16) | 없음 |

세부 내용은 `.claude/task/task-security.md` 참고.

### Subscription 도메인 문서 갱신에 따른 완료 무효화 (2026-07-15, 사용자 승인)

- `.claude/domain/SUBSCRIPTION.md`에 누락/오기 규칙이 사용자에 의해 직접 수정됨(구독권 상태 불변식 3개 신규 추가, 환불 정책 명문화, 회원 탈퇴/취소 처리 로직 변경).
- 영향 분석 결과 **SUB-01, SUB-04**를 🟢 → 🟡(재검증 필요)로 변경. 재검증 방법은 둘 다 재구현(세부 사유·수정된 완료 기준은 `task-subscription.md` 각 작업 진행 기록 참고).
- **SUB-06**은 미착수(⚪) 상태였으나 완료 기준의 근거 규칙 자체가 바뀌었고 SUB-01/SUB-04 재검증에 의존하므로 🟠(연관작업대기)로 전환, 의존성에 SUB-04 추가.
- SUB-02/SUB-03/SUB-05가 참조하는 규칙 문구는 이번 diff에서 변경되지 않아 영향 없음(그대로 🟢 유지).
- **SUB-01 재검증 완료(2026-07-15)**: 신규 불변식 3개 가드 구현·테스트 33/33 통과. 리뷰 중 발견된 blocker(환불 대상 구독권 취소 시 `SubscriptionService.cancelSubscription()`이 `suspend()` 후 `cancel()`을 호출해 새 가드에 의해 예외 발생)는 SUB-01 범위 밖으로 판단, SUB-04로 이관(사용자 확인).
- **SUB-04 재검증 완료(2026-07-15)**: 환불 정책(유료+회원의 유료 구독권 정확히 1개+활성+14일 이내) 판정 로직(`isEligibleForRefund`)을 신규 도입해 `cancelSubscription` 재작성, `SubscriptionRepository.findByMemberId` 추가. SUB-01의 blocker가 `cancel()` 미호출 경로로 실제 해소됨을 확인. 테스트 10/10(전체 PASS), PMD 0건. 이로써 SUB-01/SUB-04 모두 🟢 복귀 — 임시 회귀 창 종료.
- 위 완료에 따라 SUB-06(의존성: SUB-01, SUB-04, MEMBER-05, MEMBER-04)이 모두 충족되어 🟠 → ⚪로 갱신.

### 크로스커팅 발견 사항 처리 결과 (2026-07-13)

- **전역 테스트 라인 커버리지 79% (목표 80%, `test-code-runner-agent` 기준)**: MEMBER-02 재구현 완료 시 `./gradlew check` 실행 중 발견. MEMBER-02 자체 diff(`MemberRegisteredEvent`)는 4/4줄(100%) 커버됨 — 원인은 이 작업과 무관한 기존 미테스트 영역: `security/jwt`(19.8%), `security/refresh`(21.2%), `security/principal`(0%), `security/exception`(0%), `security/service`(25%), `member/service`의 `login()` 등.
  - `security/*` 영역 → 사용자 확인(2026-07-13)으로 Security를 경량 BC로 등록하고 SEC-01~04로 구체화(위 요약표 참고).
  - `subscription/infrastructure`(23.3%, `DummyPaymentGateway` 등 결제 stub) → 사용자 확인(2026-07-13)으로 지금 별도 작업 등록하지 않음. SUB-03(결제/환불 이벤트 기반 아키텍처 전환) 계획 수립 시 이 stub이 재작성될 가능성이 높아 그때 함께 반영 제안할 것.
  - `member/service`의 `login()` 등 Member 자체 커버리지 공백은 이번에 작업으로 등록하지 않음(추후 필요 시 별도 확인).

### 크로스커팅 발견 사항 처리 결과 (2026-07-14)

- **`Long.parseLong(authentication.getName())` 인증 사용자 ID 추출 버그**: COURSE-08b(AOP 방식 재구현) 통합 테스트가 실제 JWT 인증 흐름(이메일 기반 `CustomUserPrincipal`)으로 검증하는 과정에서 발견. `Authentication.getName()`은 principal이 `UserDetails`면 `getUsername()`(이메일)을 반환하므로, 이 패턴은 실제 운영 환경에서 항상 `NumberFormatException`을 던진다. 기존 테스트들은 전부 `@WithMockUser(username = "<숫자>")`로 인위적인 숫자 username을 사용해 이 버그를 가려왔음.
  - `course/controller/CourseController.createCourse()` → COURSE-08b 범위에서 `CustomUserPrincipal.getUserId()` 사용으로 수정 완료(2026-07-14).
  - `member/controller/MemberSelfController.java` → 사용자 확인(2026-07-14)으로 지금 함께 수정(MEMBER-04 완료 무효화 절차 진행 중).
  - `subscription/presentation/SubscriptionController.java`(2곳) → 사용자 확인(2026-07-14)으로 지금 수정하지 않음. Subscription BC는 이미 전면 재설계가 확정되어 있으므로(TASK.md 상단 BC 로드맵 참고), SUB-01~04 착수 시 이 결함도 함께 반영할 것.
  - **후속(2026-07-16)**: SUB-04 완료 근거를 재확인한 결과 이 결함이 실제로는 반영되지 않고 남아있음(수동 생성/재발급 엔드포인트 제거만 수행됨, 남은 1곳은 `cancel()`). SUB-01~07 전체 완료 후 SUB-08로 구체화하여 정식 작업으로 등록(위 요약표 참고).

### 크로스커팅 발견 사항 처리 결과 (2026-07-16, Course)

- **`CourseController`의 `(CustomUserPrincipal) authentication.getPrincipal()` 무조건 캐스팅**: SUB-08(`SubscriptionController`) 완료 직후, 사용자 요청으로 동일 패턴이 있는 다른 BC를 확인하는 과정에서 발견. 11개 메서드가 개별 인라인으로 캐스팅해 실패 시 `ClassCastException`(500)이 발생할 수 있었음. SUB-08과 동일하게 `instanceof` 검사 + 도메인 예외로 방어하도록 COURSE-11로 등록.

### 크로스커팅 발견 사항 처리 결과 (2026-07-16, Member)

- **`MemberSelfController.resolveMemberId()`의 `(CustomUserPrincipal) authentication.getPrincipal()` 무조건 캐스팅**: 위와 동일한 확인 과정에서 발견(1곳, 헬퍼 경유). SUB-08과 동일하게 `instanceof` 검사 + 도메인 예외로 방어하도록 MEMBER-07로 등록.
- **Member BC에 `MemberException`을 처리하는 예외 핸들러가 전혀 없음(신규 발견)**: 전체 코드베이스 확인 결과 `SubscriptionExceptionHandler`/`CourseExceptionHandler`(각각 `@RestControllerAdvice`, 400 매핑)는 존재하나 Member BC에는 대응하는 핸들러가 없어, 기존에도 `MemberService`가 던지는 `MemberException`(회원가입 중복 이메일 등)이 전부 처리되지 않고 500으로 노출되고 있었음(관련 컨트롤러 레벨 테스트 부재로 지금까지 발견되지 않음). MEMBER-07에서 신규 `MemberExceptionHandler`를 추가해 이번 instanceof 가드뿐 아니라 기존 `MemberException` 전체가 400으로 일관되게 처리되도록 함께 반영(사용자 확인, 2026-07-16).

### Member 관련 🟠 대기 항목 (타 BC 계획 시 등록 예정, 지금 임의 생성하지 않음)

- ~~Subscription BC: `MemberSuspendedEvent`/`MemberWithdrawnEvent` 리스너~~ → 2026-07-13 Subscription BC 계획 수립 시 SUB-06으로 구체화(자리표시자 치환 완료).
- ~~Course BC: `InstructorSuspendedEvent` 리스너 + 정지 강사 2차 방어 인터셉터~~ → 2026-07-13 Course BC 계획 수립 시 COURSE-08a/08b로 구체화(자리표시자 치환 완료).

### 도메인별 개요

- **회원 관리 (Member)**: 회원 가입/인증/정지/탈퇴, 강사 프로필, 어드민 강사 관리. 기존 구현 내역은 `.claude/PROGRESS.md`(레거시, 대조 전용) 참고.
- **강좌 컨텐츠 관리 (Course)**: 강좌/강의/미션 CRUD 및 공개·비공개는 기존 구현 존재(대부분 🟢). soft delete, 순서(Sortable) 관리, 강의 자료 타입, 정지 강사 방어는 신규 구현 필요(⚪). COURSE-01은 강의/미션 초기 생성 상태를 도메인 문서 기준(PUBLIC)으로 맞추는 수정이 필요해 ⚪로 시작.
- **구독권 결제 관리 (Subscription)**: 기존 구현(2026-06-23)이 도메인 문서와 상태 표현·재발급 이력·유효기간 계산·가격·결제 아키텍처에서 크게 달라 전면 재설계로 확정(2026-07-13 사용자 확정). SUB-01~07 전체 완료(2026-07-15, 중간에 도메인 문서 갱신에 따른 SUB-01/SUB-04 재검증 포함). 2026-07-16: 2026-07-14에 발견되고 보류됐던 크로스커팅 인증 버그(`SubscriptionController`)를 SUB-08로 구체화해 완료(instanceof 기반 principal 검증 포함) — Subscription BC 계획 항목 모두 🟢.
- **Security (공통 인프라)**: JWT 인증/인가·리프레시 토큰·UserDetailsService는 이미 구현되어 있으나 단위 테스트가 전혀 없었음(2026-07-13 발견). 새 기능 없이 기존 코드에 대한 테스트 커버리지 보강만 진행(SEC-01~04).
