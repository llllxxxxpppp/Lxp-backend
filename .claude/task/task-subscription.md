# task-subscription.md

구독권 결제 관리(Subscription) 바운디드 컨텍스트 세부 작업 파일이다. 상태의 원본은 `.claude/TASK.md` 요약표이며, 본 파일에는 상태를 중복 기록하지 않는다.

도출 근거: `.claude/domain/SUBSCRIPTION.md` + `.claude/ARCHITECTURE.md`(구독권 헥사고날 구조).
대조 근거: `.claude/PROGRESS.md` 구독권 결제 관리 바운디드 컨텍스트 섹션 + 실제 코드 확인(2026-07-13, 이번 세션, `src/main/java/com/lcs/lxp/subscription/**`).

## 계획 수립 시 확정된 사항 (2026-07-13, 사용자 확정)

기존 구현(2026-06-23)은 `SUBSCRIPTION.md`와 다음 지점에서 크게 달랐고, 전부 "문서 기준으로 재구현" 방향으로 확정됨:

1. **재발급 이력 체인**: 부모 구독권 ID + 구독 시작 일시(재발급 시 최초 값 복사 전파) + 구독 회차 도입 (기존: 이력 없이 discard 후 신규 생성만). (2026-07-14 정정: 최초 "루트 구독권 ID로 참조 후 그 생성일시 조회" 방식에서 "구독 시작 일시를 각 구독권이 직접 복사 소지" 방식으로 변경, 루트 구독권 ID 필드 제거.)
2. **유효기간 계산**: 달력 월 기준(가변 일수)으로 변경 (기존: 고정 31일).
3. **가격**: 19,800원으로 변경 (기존: 9,900원).
4. **결제/환불 아키텍처**: 이벤트 기반으로 전환하되, 결제 요청 리스트와 환불 요청 리스트를 분리하지 않고 **하나의 통합 리스트**로 관리(Payment의 RequestType으로 구분) — `SUBSCRIPTION.md` 문서 수정 완료.

기존 구현 대부분이 이 재설계로 대체되므로, 아래 작업 목록에는 기존 코드 기반 🟢 판정 항목이 없다(전부 신규/재작업).

미해결 확인 필요 사항(작업 진행 중 재확인):
- `SUBSCRIPTION.md` "회원 취소 이벤트를 받으면"(114행 부근) — Member BC에는 "회원 취소" 개념이 없고 "정지"/"탈퇴"만 존재. 문맥(첫 유료 결제 2주 이내 처리)상 "회원 탈퇴 이벤트"의 오기로 추정. SUB-06 착수 시 사용자에게 재확인 후 진행.

---

## [SUB-01] 구독권 애그리거트 재설계 (상태 표현 전환 + 재발급 체인 + 유효기간 계산)

**설명**: `Subscription`의 상태 표현을 단일 enum에서 활성화/정지/취소 각각의 nullable 일시로 전환. 부모 구독권 ID, 구독 시작 일시, 구독 회차 필드 추가. `reissue()` 복사 생성자 도입. 유효기간을 달력 월 기준으로 계산.

**완료 기준**:
- 생성 시 부모 구독권 ID = 0(자신이 루트), 구독 시작 일시 = 객체 생성 시 `OffsetDateTime.now()`로 주입, 구독 회차 = 1
- `reissue()`: 회차 = 원본+1, 부모 ID = 원본 id, 구독 시작 일시 = 원본의 값을 그대로 복사(재계산하지 않음)
- 유효기간 = `(구독 시작 일시).plusMonths(구독 회차).plusDays(1).truncatedTo(DAYS)` (말일 처리 포함), 재발급 시 계산 후 불변값으로 저장
- `isValid()`: 활성화 true && 정지 false && 유효기간 안지남 (취소 여부 무관)
- `isEligibleForReissue()`: 활성화 true && 정지 false && 취소 false && 만료 2일 전
- 활성화/정지/취소 일시는 생성 시 NULL, 해당 액션 시에만 채워짐

**관련 규칙 위치**: `.claude/domain/SUBSCRIPTION.md` "구독권 루트 애그리거트 도메인 모델 규칙"

**대상 파일**: `domain/model/entity/Subscription.java`, `domain/repository/SubscriptionRepository.java`

**진행 기록**: (미착수) — 기존 코드는 `SubscriptionStatus` enum 단일 필드 + 고정 31일 만료로 구현되어 있어 전면 재작성 대상(`Subscription.java` 전체 확인, 2026-07-13). 2026-07-14: `SUBSCRIPTION.md` 갱신(커밋 c37f920)에 맞춰 완료 기준의 "루트 구독권 ID" 관련 항목을 "구독 시작 일시" 필드 기반으로 수정(루트 구독권 ID 필드 자체가 삭제됨).

**완료 (2026-07-14)**:
- 테스트: `src/test/java/com/lcs/lxp/subscription/domain/SubscriptionTest.java` 전면 재작성(BDD 네이밍, `@DisplayName` 준수).
- 구현: `Subscription.java` 전면 재작성(활성화/정지/취소 각각 nullable `OffsetDateTime`, 부모 구독권 ID, 구독 시작 일시, 구독 회차, `reissue()`, 달력 월 유효기간 계산). `SubscriptionRepository.java`는 삭제된 필드(`status`, `expiresAt`) 참조 파생 쿼리 제거.
- 리뷰 1차: major 1건 — `isEligibleForReissue()`가 이미 만료된 지 오래된 구독권도 재발급 대상으로 오판정(하한 조건 누락). `isNearExpiry()`에 `!isExpired()` 하한 추가로 수정, 회귀 테스트(`givenActivatedButAlreadyExpiredLongAgo_whenIsEligibleForReissue_thenReturnsFalse`) 추가.
- 리뷰 2차: PASS (minor 1건 기록만 — `isNearExpiry()` 내 `now` 호출이 완전히 단일 스냅샷은 아님, 기능 영향 없음).
- 테스트 실행: `SubscriptionTest` 23/23 PASS. `SubscriptionService`/`SubscriptionController`/`SubscriptionResponse`와 기존 테스트(`PaymentTest`, `SubscriptionServiceTest`, `SubscriptionControllerTest`)는 옛 API 참조로 컴파일이 깨진 상태(SUB-04 범위, 사용자 확인 완료) — 이번 테스트 실행 시 해당 파일들을 임시로 컴파일 대상에서 제외한 뒤 실행하고 원상 복구함(`git status`로 원복 확인).
- 완료 근거: 리뷰 승인 + 테스트 23/23 통과 + 사용자 확인(2026-07-14).

---

## [SUB-02] Payment 애그리거트를 요청 리스트 구조로 재설계

**설명**: `Payment`를 Subscription 1:1에서 1:N(리스트)로 전환. `RequestType`(PAYMENT/REFUND) 불변 필드, 요청전송일시·응답수신일시·`ResponseResult`(NOT_REQUESTED/SUCCESS/FAILED) 가변 필드로 재구성. 결제 요청과 환불 요청을 하나의 리스트로 통합 관리(계획 수립 시 확정 사항 4번).

**완료 기준**:
- `Payment`는 멱등키(RequestId, UUIDv4) + `RequestType` 불변, 요청전송/응답수신일시 + `ResponseResult` 가변
- `Subscription`은 `List<Payment>` 하나로 결제/환불 요청 내역 보유
- 기존 `PaymentStatus`(결제/환불 단계를 한 enum에 합친 것) 대체 또는 `RequestType`+`ResponseResult` 조합으로 재구성

**관련 규칙 위치**: `.claude/domain/SUBSCRIPTION.md` "공통 VO"(RequestId, RequestType, ResponseResult), "결제 애그리거트 도메인 모델 규칙", "구독권 루트 애그리거트 도메인 모델 규칙"(통합 리스트, 2026-07-13 정정)

**대상 파일**: `domain/model/entity/Payment.java`, `domain/model/vo/RequestType.java`(신규), `domain/model/vo/ResponseResult.java`(신규), `domain/model/entity/Subscription.java`

**의존성**: SUB-01

**진행 기록**: (미착수)

---

## [SUB-03] 결제/환불 이벤트 기반 아키텍처 전환

**설명**: `SubscriptionService`가 `PaymentAdapter`를 직접 호출하던 방식을 "결제 요청 이벤트"/"환불 요청 이벤트" 발행 방식으로 전환. `PaymentAdapter`는 이벤트 리스너로 동작하며 처리 결과를 다시 도메인에 반영.

**완료 기준**: 결제/환불 요청 시 각각 이벤트(구독권 id, 결제 id 포함) 발행, `PaymentAdapter`가 이벤트를 구독해 PG 어댑터(stub) 호출, 처리 결과가 해당 Payment/Subscription에 반영됨(결제 성공→활성화, 결제 실패→추가 행동 없음, 환불 성공→정지, 환불 실패→추가 행동 없음)

**관련 규칙 위치**: `.claude/domain/SUBSCRIPTION.md` "결제 어댑터 비즈니스 규칙", "이벤트 규칙 > 결제 요청 이벤트/환불 요청 이벤트", "구독권 서비스 비즈니스 규칙"(결제·환불 응답 처리)

**대상 파일**: `domain/event/PaymentRequestedEvent.java`(신규), `domain/event/RefundRequestedEvent.java`(신규), `infrastructure/PaymentAdapter.java`, `application/service/SubscriptionService.java`

**의존성**: SUB-02

**진행 기록**: (미착수) — 응답을 다시 도메인에 반영하는 구체적 흐름(동기 반환 vs 응답 이벤트)은 착수 시 설계 확정 필요.

---

## [SUB-04] 구독권 생성/조회/취소 API 정합성 조정 (가격 19,800원 반영)

**설명**: SUB-01~03 재설계에 맞춰 생성/조회/취소 API와 응답 DTO를 정합화. 가격 상수를 19,800원으로 변경.

**완료 기준**: 유료 구독권 생성 시 19,800원 청구, `SubscriptionResponse`에 부모 구독권 ID·구독 시작 일시·구독 회차 포함, 취소 시 환불 조건(14일 이내) 로직 유지

**관련 규칙 위치**: `.claude/domain/SUBSCRIPTION.md` "구독권 루트 애그리거트 도메인 모델 규칙"(가격), "구독권 서비스 비즈니스 규칙"

**대상 파일**: `application/service/SubscriptionService.java`, `application/dto/response/SubscriptionResponse.java`, `presentation/SubscriptionController.java`

**의존성**: SUB-01, SUB-02

**진행 기록**: (미착수)

---

## [SUB-05] 회원가입 이벤트 리스너 (무료 구독권 자동 발급)

**설명**: `MemberRegisteredEvent`(Member BC 발행 예정)를 구독해 신규 회원에게 무료 구독권을 자동 발급.

**완료 기준**: 이벤트 수신 시 해당 회원에게 무료(0원) 구독권 생성 및 즉시 활성화

**관련 규칙 위치**: `.claude/domain/SUBSCRIPTION.md` "구독권 서비스 비즈니스 규칙"(회원가입 이벤트 → 무료 구독권 발급)

**대상 파일**: `domain/event/MemberRegisteredEventListener.java`(신규), `application/service/SubscriptionService.java`

**의존성**: SUB-01, MEMBER-02(🟡 — `MemberRegisteredEvent` 발행 재구현 진행 중)

**진행 기록**: (미착수)

---

## [SUB-06] 회원 정지/탈퇴 이벤트 리스너 (구독권 정지·취소 처리)

**설명**: `MemberSuspendedEvent`/`MemberWithdrawnEvent`를 구독해 구독권을 정지 또는 취소(+조건부 환불) 처리. 기존 `SubscriptionService.suspendSubscription(memberId)`와 연결.

**완료 기준**:
- 정지 이벤트 수신 시 활성 구독권 정지
- 탈퇴 이벤트 수신 시: 첫 유료 결제 2주 이내면 정지+결제 취소(환불), 아니면 구독권 취소

**관련 규칙 위치**: `.claude/domain/SUBSCRIPTION.md` "구독권 서비스 비즈니스 규칙"(회원 정지/탈퇴 이벤트 처리)

**대상 파일**: `domain/event/MemberSuspendedEventListener.java`(신규), `domain/event/MemberWithdrawnEventListener.java`(신규), `application/service/SubscriptionService.java`

**의존성**: SUB-01, MEMBER-05(🟢), MEMBER-04(🟢)

**진행 기록**: (미착수) — `TASK.md`의 "Subscription BC: MemberSuspendedEvent/MemberWithdrawnEvent 리스너" 자리표시자를 이번 계획에서 SUB-06으로 구체화함(치환 근거: MEMBER-05, MEMBER-04 모두 🟢로 완료되어 의존성 충족). "회원 취소 이벤트" 표현의 재확인 필요 사항은 상단 참고.

---

## [SUB-07] 만료 임박 구독권 자동 재발급 배치

**설명**: 매일 0시 재발급 대상 구독권(만료 2일 전, 활성·비정지·비취소)을 조회해 자동 재발급하는 스케줄러 도입. 기존 수동 트리거 엔드포인트(`POST /api/subscriptions/reissue`)는 유지 여부 착수 시 결정.

**완료 기준**: `@Scheduled` cron으로 매일 0시 실행, `Subscription.isEligibleForReissue()` 대상 전체에 대해 `reissue()` 수행 및 결제 요청 흐름 연동

**관련 규칙 위치**: `.claude/domain/SUBSCRIPTION.md` "구독권 서비스 비즈니스 규칙"(매일 0시 재발급 배치)

**대상 파일**: `infrastructure/SubscriptionReissueScheduler.java`(신규), `application/service/SubscriptionService.java`

**의존성**: SUB-01, SUB-03

**진행 기록**: (미착수)
