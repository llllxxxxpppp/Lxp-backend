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

해소된 확인 사항: 위 "회원 취소 이벤트" 오기 여부는 2026-07-15 `SUBSCRIPTION.md` 갱신으로 "회원 탈퇴 이벤트"로 확정되었고, 처리 로직(활성화된 모든 구독권 정지 + 환불 정책 통과 시 환불, 취소 로직 없음)도 함께 변경됨 — SUB-06 완료 기준 참고.

## 도메인 문서 갱신에 따른 완료 무효화 (2026-07-15, 사용자 승인)

`.claude/domain/SUBSCRIPTION.md`에 다음 규칙이 신규/변경됨:
1. 구독권 상태 불변식 3개 추가 — (a) 활성화 이후에만 정지/취소 가능 (b) 정지·취소 상태 상호배타 (c) 동일 상태변경 중복 호출 시 예외.
2. "환불 정책"이 별도 규칙으로 명문화 — 회원의 유료 구독권이 정확히 1개 + 해당 구독권이 활성 상태 + 활성화 후 14일 이내.
3. 회원 정지 이벤트 처리: "구독권을 정지한다" → "활성화된 **모든** 구독권을 정지한다".
4. 회원 탈퇴 이벤트 처리 전면 변경: 기존 "첫 유료 결제 2주 이내면 정지+환불, 아니면 취소" → "활성화된 모든 구독권 정지 + 환불 정책 통과 시 환불 실시"(취소 로직 삭제됨).
5. 회원의 자진 구독권 취소(`cancelSubscription`) 흐름 신설: 환불 정책 만족 시 환불 + 즉시 정지(`cancel()` 호출 안 함), 불만족 시 취소만(`suspend()` 호출 안 함).

영향 분석(근거: 실제 코드 확인, 2026-07-15):
- **SUB-01**: 1번 규칙을 반영하는 애그리거트 규칙이 대상. 현재 `Subscription.activate()`/`suspend()`/`cancel()`은 가드 없이 값을 덮어쓰는 구조라 세 불변식 모두 미반영 → 🟡(재검증 필요), 재구현 필요.
- **SUB-04**: 2·5번 규칙을 반영하는 취소 API가 대상. 현재 `SubscriptionService.cancelSubscription()`은 "유료+14일 이내 → 환불이벤트 발행 후 항상 `cancel()`"로 구현되어 있어 신규 규칙과 상충 → 🟡(재검증 필요), 재구현 필요.
- **SUB-06**: 3·4번 규칙이 완료 기준의 근거이나 아직 미착수(⚪)였으므로 완료 무효화 대상은 아님. 다만 완료 기준 자체가 바뀌었고 SUB-04가 구현할 "환불 정책 판정" 로직을 재사용해야 하므로 🟠(연관작업대기)로 전환, 의존성에 SUB-04 추가.
- SUB-02/SUB-03/SUB-05가 참조하는 규칙 문구는 이번 diff에서 변경되지 않아 영향 없음.

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

**완료 기준 추가 (2026-07-15, 도메인 문서 갱신에 따른 재검증 대상)**:
- `activate()`: 이미 활성화된 상태(activatedAt != null)에서 호출 시 예외 발생
- `suspend()`: 활성화되지 않은 상태에서 호출 시 예외, 이미 정지된 상태에서 호출 시 예외, 이미 취소된 상태에서 호출 시 예외(정지·취소 상호배타)
- `cancel()`: 활성화되지 않은 상태에서 호출 시 예외, 이미 취소된 상태에서 호출 시 예외, 이미 정지된 상태에서 호출 시 예외(정지·취소 상호배타)

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

**재검증 필요 (2026-07-15, 사용자 승인)**: `SUBSCRIPTION.md` 갱신으로 위 "완료 기준 추가" 3개 불변식이 신규 반영됨. 현재 구현(`activate()`/`suspend()`/`cancel()`)은 어떤 가드도 없이 필드를 덮어쓰는 구조라 세 불변식 모두 미반영(코드 확인 완료). 재검증 방법: 재구현(세 메서드에 가드 추가) + 신규 예외 케이스 테스트(`SubscriptionTest`) 추가. 착수 전 SUB-02~SUB-05가 이 메서드들을 단일 호출로만 사용해 정상 흐름에서는 예외가 발생하지 않음을 재확인 완료(가드 추가만으로 회귀 없음).

**재검증 완료 (2026-07-15)**:
- 테스트: `SubscriptionTest.java`에 신규 불변식 검증 7종 추가(`givenAlreadyActivatedSubscription_whenActivateAgain_thenThrowsException` 등, activate/suspend/cancel 각각의 중복 호출·활성화 전 호출·정지·취소 상호배타 케이스).
- 구현: `Subscription.java`의 `activate()`/`suspend()`/`cancel()`에 기존 private 헬퍼(`isActivated()`/`isSuspended()`/`isCancelled()`)를 재사용한 가드 추가, 위반 시 기존 `SubscriptionException` 발생(신규 예외 클래스 도입 없음).
- 리뷰: 1차 blocker 1건 — `SubscriptionService.cancelSubscription()`의 환불 대상 취소 흐름(유료+14일 이내)에서, 동기 이벤트 처리로 `PaymentAdapter`가 같은 트랜잭션·같은 영속성 컨텍스트에서 동일 `Subscription` 인스턴스에 `suspend()`를 호출한 뒤, `cancelSubscription()`이 이어서 같은 인스턴스에 `cancel()`을 호출해 새 "정지·취소 상호배타" 가드에 걸려 예외가 발생함을 확인. **판정(사용자 확인, 2026-07-15)**: 이는 SUB-01의 결함이 아니라, 새 도메인 규칙("회원이 구독권을 취소하는 경우 — 환불 정책 만족 시 환불+즉시 정지, `cancel()` 호출 안 함")이 이미 명시한 대로 SUB-04에서 고쳐야 할 로직이 새 불변식에 의해 드러난 것으로 판단 — SUB-01 범위(대상 파일: `Subscription.java`)를 넘어서는 수정이라 이 작업에서 처리하지 않음. **알려진 임시 회귀 (SUB-04 완료 전까지)**: `POST /api/subscriptions/{id}/cancel`을 유료+환불기간 이내 구독권에 호출하면 `SubscriptionException`이 발생한다(정상 케이스가 예외로 깨짐) — SUB-04 재작업 완료 시 해소 예정, 사용자 확인 후 SUB-01을 완료 처리하고 SUB-04에 바로 착수하기로 함.
- 리뷰 2차: 위 항목을 "SUB-01 범위 밖, SUB-04에서 해결"로 확정한 뒤 그 외 지적 없음 — 가드 로직·테스트 네이밍(BDD)·PMD 위반 모두 통과.
- 테스트 실행: `SubscriptionTest` 33/33 PASS(신규 7종 포함), 전체 테스트 PASS, PMD 위반 0건.
- 완료 근거: 리뷰 승인(알려진 범위 밖 이슈 1건은 SUB-04로 이관, 사용자 확인) + 테스트 33/33 통과 + PMD 0건 + 사용자 확인(2026-07-15).

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

**완료 (2026-07-14)**:
- 테스트: `PaymentTest.java` 전면 재작성, `RequestIdTest.java`/`RequestTypeTest.java`/`ResponseResultTest.java` 신규 작성(BDD 네이밍, `@DisplayName` 준수).
- 구현: `RequestId`(UUID 기반 VO + `generate()`), `RequestIdConverter`(JPA 컨버터), `RequestType`(PAYMENT/REFUND), `ResponseResult`(NOT_REQUESTED/SUCCESS/FAILED) 신규. `Payment.java` 전면 재작성(멱등키/요청타입 불변, 요청전송·응답수신일시·응답결과 가변, `markRequested()`/`markResponded()`). `Subscription.java`에 `List<Payment> payments` 필드 + `addPayment()`/`getPayments()`(불변 뷰) 추가(SUB-01 기존 로직 변경 없음, `reissue()` 시 독립적인 빈 리스트로 시작 확인).
- 리뷰: 1차 승인(blocker/major 없음, minor 1건은 `RequestId` 생성자가 v4 형식 자체를 강제하지 않음 — 실제 생성 경로는 항상 `generate()`로 v4 보장되어 기능 영향 없어 기록만).
- 테스트 실행: `PaymentTest`(16) + `RequestIdTest`(5) + `RequestTypeTest`(4) + `ResponseResultTest`(5) + `SubscriptionTest`(23) = 53/53 PASS. 도메인 패키지 커버리지 80% 이상(Payment 91%, Subscription 98%, RequestId/RequestType/ResponseResult 100%). `SubscriptionService`/`SubscriptionController`/`SubscriptionResponse`/`PaymentAdapter`/`PaymentResult`와 기존 테스트 2개는 옛 API 참조로 컴파일이 깨진 상태(SUB-03/04 범위, 사용자 확인 완료) — 이번 테스트 실행 시 해당 파일들을 임시로 컴파일 대상에서 제외한 뒤 실행하고 원상 복구함(`git status`로 원복 확인).
- 미사용 정리 보류: `PaymentInfo`/`PaymentStatus`/`PaymentSuccessResponse`/`PaymentFailureResponse`/`RefundInfo`/`RefundSuccessResponse`/`RefundFailureResponse`는 새 `Payment` 설계로 더 이상 쓰이지 않으나, `PaymentAdapter`/`PaymentResult`/`SubscriptionService`가 아직 참조 중이라 삭제하지 않고 남겨둠(SUB-03/04에서 함께 정리 예정).
- 완료 근거: 리뷰 승인 + 테스트 53/53 통과 + 사용자 확인(2026-07-14).

---

## [SUB-03] 결제/환불 이벤트 기반 아키텍처 전환

**설명**: `SubscriptionService`가 `PaymentAdapter`를 직접 호출하던 방식을 "결제 요청 이벤트"/"환불 요청 이벤트" 발행 방식으로 전환. `PaymentAdapter`는 이벤트 리스너로 동작하며 처리 결과를 다시 도메인에 반영.

**완료 기준**: 결제/환불 요청 시 각각 이벤트(구독권 id, 결제 id 포함) 발행, `PaymentAdapter`가 이벤트를 구독해 PG 어댑터(stub) 호출, 처리 결과가 해당 Payment/Subscription에 반영됨(결제 성공→활성화, 결제 실패→추가 행동 없음, 환불 성공→정지, 환불 실패→추가 행동 없음)

**관련 규칙 위치**: `.claude/domain/SUBSCRIPTION.md` "결제 어댑터 비즈니스 규칙", "이벤트 규칙 > 결제 요청 이벤트/환불 요청 이벤트", "구독권 서비스 비즈니스 규칙"(결제·환불 응답 처리)

**대상 파일**: `domain/event/PaymentRequestedEvent.java`(신규), `domain/event/RefundRequestedEvent.java`(신규), `infrastructure/PaymentAdapter.java`, `application/service/SubscriptionService.java`

**의존성**: SUB-02

**설계 확정 (2026-07-14, 사용자 승인)**:
- 응답 반영 흐름: 별도의 "응답 이벤트"를 두지 않고, `PaymentAdapter`가 `@EventListener`로 요청 이벤트를 구독해 PG stub 호출 후 그 결과를 동일 리스너 내에서 동기적으로 `Payment`/`Subscription`에 반영한다(발행 트랜잭션과 동일 스레드에서 동기 실행되는 Spring 기본 `@EventListener` 동작에 의존).
- 결제 이벤트(`PaymentRequestedEvent`)는 `SubscriptionService.createSubscription()`의 유료 구독권 생성 흐름에 실제로 연결한다(도메인 문서에 명시된 유일한 트리거).
- 환불 이벤트(`RefundRequestedEvent`)는 이번 작업에서 클래스와 `PaymentAdapter` 리스너 메커니즘까지만 준비하고, 실제 발행 호출부는 만들지 않는다. 도메인 문서상 환불 트리거는 "회원 취소(탈퇴) 이벤트 처리"(SUB-06, 첫 유료 결제 2주 이내 정지+환불) 문맥에만 명시되어 있고, SUB-04의 "취소 시 환불 조건(14일 이내) 로직 유지"는 기존 구현에서 넘어온 항목이라 이번 작업에서 임의로 재현하지 않는다(SUB-04/SUB-06에서 실제 호출부 연결 예정).
- `SubscriptionService`는 `createSubscription(Long memberId, Long price)`(가격을 직접 입력받는 방식 — 무료/유료 판단 로직은 API 정합성 조정인 SUB-04 범위), `cancelSubscription(Long memberId, Long subscriptionId)`(환불 조건 없이 단순 취소만), `getSubscriptionInfo(Long subscriptionId)`만 새 도메인 모델 기준으로 재작성한다. `suspendSubscription`/`reissueExpiring`은 옛 API(`SubscriptionStatus`, `discard()` 등, SUB-01에서 이미 제거됨)를 참조하고 있고 각각 SUB-06/SUB-07이 소유할 기능이라 이번 작업에서 제거한다(재작성 시 새 리포지토리 쿼리·비즈니스 규칙을 임의로 선점하지 않기 위함). 해당 메서드는 SUB-06/SUB-07 착수 시 새로 추가된다.
- 대상 파일에 `application/dto/response/SubscriptionResponse.java`를 추가한다(완료 기준에 없었으나, 프로젝트 규칙상 Service 리턴값은 DTO여야 하므로 `createSubscription`/`getSubscriptionInfo`가 컴파일되려면 필수. 필드를 새 `Subscription` 구조에 맞게 매핑만 하는 최소 수정이며, API 응답의 의미·가격 정합성 조정은 SUB-04에서 추가로 다룸).

**완료 (2026-07-14)**:
- 테스트: `SubscriptionServiceTest.java` 전면 재작성(`createSubscription`/`cancelSubscription`/`getSubscriptionInfo`만 다룸, `suspendSubscription`/`reissueExpiring` 테스트 제거), `PaymentAdapterTest.java` 신규.
- 구현: `PaymentRequestedEvent`/`RefundRequestedEvent`(`BaseDomainEvent` 상속) 신규. `SubscriptionService`를 이벤트 발행 방식으로 전면 재작성(`createSubscription(memberId, price)`: 무료→즉시활성화·이벤트미발행, 유료→`PaymentRequestedEvent` 발행; `cancelSubscription`: 환불 조건 없이 단순 취소; `getSubscriptionInfo`; `suspendSubscription`/`reissueExpiring`은 제거 — SUB-06/SUB-07에서 재작성 예정). `PaymentAdapter`를 `@EventListener` 기반으로 전면 재작성(`handlePaymentRequested`/`handleRefundRequested`, PG stub 호출 후 동기 반영). `SubscriptionResponse`를 새 `Subscription` 필드 기준으로 재매핑.
- 범위 확장(설계 확정 메모 참고): `Subscription.java`에 `markPaymentRequested(Long)`/`markPaymentResponded(Long, ResponseResult)`/`findPayment(Long)`(private) 캡슐화 메서드 추가(기존 필드·메서드는 변경 없음 — Payment는 애그리거트 자식이므로 Subscription 루트를 통해서만 변경하도록 함). `application/dto/response/SubscriptionResponse.java`도 최소 매핑 수정으로 대상 파일에 포함. 미사용이 된 `infrastructure/PaymentResult.java`는 삭제(사용자 확인, 2026-07-14).
- 리뷰: 1차 승인(blocker/major 없음, minor 2건 — 무료 생성 분기의 직접 Payment 접근과 캡슐화 방식 간 미세한 일관성 차이, 테스트 일부의 `verify()` 누락. 둘 다 기능 결함 아님, 기록만).
- 테스트 실행: `SubscriptionServiceTest`(6) + `PaymentAdapterTest`(6) + `PaymentTest`(16) + `RequestIdTest`(5) + `RequestTypeTest`(4) + `ResponseResultTest`(5) + `SubscriptionTest`(23) = 65/65 PASS. `SubscriptionController`/`SubscriptionControllerTest`는 옛 API(`createSubscription(Long)` 단일 인자, `reissueExpiring()`) 참조로 컴파일이 깨진 상태(SUB-04 범위, 사용자 확인 완료) — 이번 테스트 실행 시 임시로 컴파일 대상에서 제외한 뒤 실행하고 원상 복구함(`git status`로 원복 확인).
- 완료 근거: 리뷰 승인 + 테스트 65/65 통과 + 사용자 확인(2026-07-14).

---

## [SUB-04] 구독권 생성/조회/취소 API 정합성 조정 (가격 19,800원 반영)

**설명**: SUB-01~03 재설계에 맞춰 생성/조회/취소 API와 응답 DTO를 정합화. 가격 상수를 19,800원으로 변경.

**완료 기준**: 유료 구독권 생성 시 19,800원 청구, `SubscriptionResponse`에 부모 구독권 ID·구독 시작 일시·구독 회차 포함, 취소 시 환불 조건(14일 이내) 로직 유지

**관련 규칙 위치**: `.claude/domain/SUBSCRIPTION.md` "구독권 루트 애그리거트 도메인 모델 규칙"(가격), "구독권 서비스 비즈니스 규칙"

**대상 파일**: `application/service/SubscriptionService.java`, `application/dto/response/SubscriptionResponse.java`, `presentation/SubscriptionController.java`, `domain/model/entity/Subscription.java`(환불 조건 판단 메서드 복원)

**의존성**: SUB-01, SUB-02

**계획 변경 (2026-07-14, 사용자 승인)**: 사용자가 `SUBSCRIPTION.md` "구독권 서비스 비즈니스 규칙"에 "시스템에 의한 재발급이면 유료 구독권을 발급한다"를 추가함. 이를 계기로 재확인한 결과, 구독권 생성의 문서화된 트리거는 ① 회원가입 이벤트(무료, SUB-05) ② 시스템 재발급(유료, SUB-07) 두 가지뿐이며, `POST /api/subscriptions`(사용자 직접 호출 수동 생성) 엔드포인트는 도메인 문서 어디에도 근거가 없음(기존 코드에서만 존재). 사용자 확인 결과 이 엔드포인트는 SUB-04에서 **제거**하기로 결정(대안: 유지하며 19,800원 고정 — 미채택). 이에 따라 완료 기준을 아래로 수정:

**완료 기준 (수정됨)**:
- `SubscriptionController`에서 `POST /api/subscriptions`(수동 생성) 엔드포인트 제거. `SubscriptionService.createSubscription(memberId, price)` 메서드 자체는 SUB-05/SUB-07이 내부적으로 재사용할 예정이므로 유지(컨트롤러 진입점만 제거).
- `POST /api/subscriptions/reissue` 엔드포인트도 제거(대응하는 서비스 메서드 `reissueExpiring`이 SUB-03에서 이미 제거됨 — SUB-07에서 서비스 메서드와 엔드포인트를 함께 재도입).
- `cancelSubscription`: 유료(price>0)이고 활성화 후 14일 이내(`Subscription.isWithinRefundPeriod()`)이면 `Payment(REFUND)` 생성·추가·저장 후 `RefundRequestedEvent` 발행(SUB-03에서 메커니즘만 준비했던 트리거를 여기서 실제 연결) → 이후 항상 `subscription.cancel()` 수행. 무료이거나 기간 경과 시 이벤트 없이 취소만.
- `Subscription.isWithinRefundPeriod()` 복원: 활성화일 + 14일 이내(`REFUND_PERIOD_DAYS=14`, activatedAt이 null이면 false) — 옛 구현(`git show c68f44f`로 원본 확인) 그대로 복원.
- `SubscriptionResponse`의 부모 구독권 ID·구독 시작 일시·구독 회차 포함은 SUB-03에서 이미 반영 완료(회귀만 확인, 추가 변경 없음).
- 컨트롤러에는 `GET /{subscriptionId}`(조회), `POST /{subscriptionId}/cancel`(취소)만 남는다.
- 이번 작업 완료 시 subscription 패키지 전체가 처음으로 정상 컴파일 및 테스트 통과되어야 한다(더 이상 알려진 깨진 파일이 없음).
- (미채택) 가격 19,800원 상수 도입은 이번 작업에서 하지 않음 — 실제 사용 호출부(SUB-05의 무료 발급, SUB-07의 유료 재발급)가 아직 없어 지금 상수를 만들면 사용처 없는 선제적 구현이 됨. 각 작업이 착수될 때 그 작업에서 도입.

**완료 (2026-07-14)**:
- 테스트: `SubscriptionTest.java`에 `isWithinRefundPeriod()` 케이스 추가, `SubscriptionServiceTest.java`의 `cancelSubscription` 테스트를 환불 분기(정상 환불/기간 경과/무료/타인 소유) 4종으로 교체, `SubscriptionControllerTest.java` 전면 재작성(수동 생성/재발급 제거 확인 포함).
- 구현: `Subscription.java`에 `isWithinRefundPeriod()`(옛 구현 그대로 복원, `REFUND_PERIOD_DAYS=14`) 추가. `SubscriptionService.cancelSubscription`을 유료+14일 이내 환불 분기 로직으로 수정(`Payment(REFUND)` 생성→추가→저장→`RefundRequestedEvent` 발행→`cancel()`→저장). `SubscriptionController`에서 수동 생성/재발급 엔드포인트 제거, `get`/`cancel`만 유지.
- 리뷰: 1차 blocker 1건 — `POST /api/subscriptions/reissue` 제거 확인 테스트가 404를 기대했으나 `GET /{subscriptionId}` 매핑과 경로가 겹쳐 실제로는 405가 정상 동작(테스트 기대값을 405로 수정). 재검증 PASS.
- 테스트 실행: 전체 330/330 PASS(subscription 패키지 76개 포함), PMD 위반 0건, 전체 커버리지 87%. **이번이 subscription 패키지 전체가 임시 파일 제외 없이 정상 컴파일·테스트·PMD를 통과한 첫 시점.**
- 완료 근거: 리뷰 승인(재검증 포함) + 테스트 330/330 통과 + PMD 0건 + 사용자 확인(2026-07-14).

**재검증 필요 (2026-07-15, 사용자 승인)**: `SUBSCRIPTION.md` 갱신으로 환불 조건 규칙이 근본적으로 바뀜(위 "도메인 문서 갱신에 따른 완료 무효화" 섹션 2·5번 참고). 현재 구현(`cancelSubscription`)은 "유료+14일 이내 → 환불이벤트 발행 후 항상 `subscription.cancel()`"이나, 신규 규칙은 다음과 같이 달라짐:

**완료 기준 (재작성, 2026-07-15)**:
- `cancelSubscription`: "환불 정책"(회원에게 발급된 모든 구독권 중 유료 구독권이 정확히 1개 + 해당 구독권이 현재 활성 상태 + 활성화 후 14일 이내)을 만족하면 `Payment(REFUND)` 생성·추가·저장 후 `RefundRequestedEvent` 발행만 수행한다(`subscription.cancel()` 호출하지 않음 — 정지는 `PaymentAdapter`의 환불 성공 응답 처리 흐름에서 이루어짐, SUB-03에서 이미 구현됨).
- 환불 정책을 만족하지 않으면 이벤트 발행 없이 `subscription.cancel()`만 수행한다(환불 없음).
- "정확히 1개의 유료 구독권" 조건을 확인하려면 회원 소유 구독권 전체 조회가 필요 — `SubscriptionRepository`에 회원별 구독권 목록 조회 메서드 추가 필요(현재는 `existsByMemberId`만 존재).
- 환불 정책 판정 로직은 SUB-06(회원 탈퇴 처리)에서도 동일하게 재사용될 예정이므로, `SubscriptionService` 내 재사용 가능한 형태(private 메서드 등)로 구현할 것을 권고.
- `isWithinRefundPeriod()`(활성화 후 14일 이내 판단)는 SUB-01에서 이미 구현되어 있어 그대로 재사용 가능. "정확히 1개의 유료 구독권" 조건만 신규 추가 구현 필요.
- **열린 질문(착수 시 확인 필요)**: 아직 활성화되지 않은 구독권(결제 대기/실패 상태)에 대해 회원이 취소를 시도하는 경우의 처리 방식. SUB-01의 신규 불변식("활성화 이후에만 정지/취소 가능")에 따르면 `cancel()` 호출 시 예외가 발생하는데, 도메인 문서에는 이 케이스에 대한 명시가 없음. SUB-04 착수 시 사용자에게 재확인 필요.
  - **결론(2026-07-15)**: 별도 분기 없이 새 불변식대로 `SubscriptionException`이 자연스럽게 전파되도록 둔다. 결제 실패로 활성화되지 않은 구독권은 애초에 "취소"할 대상이 아니라는 도메인 모델링과 일치하는 해석으로 판단, 특별 처리를 추가하지 않음.

**재검증 완료 (2026-07-15)**:
- 테스트: `SubscriptionServiceTest.java`의 `cancelSubscription` 관련 테스트를 새 환불 정책(4조건 AND) 기준으로 전면 교체 — 환불 정책 만족(유료+단독+활성+14일 이내), 무료 구독권, 유료 구독권 2개 이상 존재, 환불기간 경과, 만료로 인한 비활성 상태 5종 + 타인 소유 취소 시도 예외(회귀) 유지.
- 구현: `SubscriptionRepository`에 `findByMemberId(Long): List<Subscription>` 추가. `SubscriptionService.cancelSubscription`을 재작성해 신규 private 메서드 `isEligibleForRefund(Subscription)`(유료 && 회원의 유료 구독권 정확히 1개 && `isValid()` && `isWithinRefundPeriod()`)로 환불 정책을 판정 — 만족 시 `Payment(REFUND)` 생성·추가·저장 후 `RefundRequestedEvent` 발행만 수행(`cancel()` 미호출), 불만족 시 이벤트 없이 `cancel()`만 수행. `isEligibleForRefund`는 SUB-06 재사용을 고려해 별도 메서드로 분리.
- 리뷰: 승인(PASS, minor 1건 — 테스트의 일부 `lenient()` 스텁이 실제로는 항상 호출되어 불필요, 기능 영향 없어 기록만). SUB-01에서 발견됐던 blocker(환불 대상 취소 시 suspend 후 cancel 호출로 예외 발생)가 이번 재작성으로 `cancel()` 미호출 경로가 되어 실제로 해소됨을 코드 경로로 확인.
- 테스트 실행: `SubscriptionServiceTest` 10/10 PASS, 전체 테스트 PASS, PMD 위반 0건, 전체 커버리지 89.63%.
- 완료 근거: 리뷰 승인 + 테스트 10/10 통과(전체 PASS) + PMD 0건 + 사용자 확인(2026-07-15).

---

## [SUB-05] 회원가입 이벤트 리스너 (무료 구독권 자동 발급)

**설명**: `MemberRegisteredEvent`(Member BC 발행 예정)를 구독해 신규 회원에게 무료 구독권을 자동 발급.

**완료 기준**: 이벤트 수신 시 해당 회원에게 무료(0원) 구독권 생성 및 즉시 활성화

**관련 규칙 위치**: `.claude/domain/SUBSCRIPTION.md` "구독권 서비스 비즈니스 규칙"(회원가입 이벤트 → 무료 구독권 발급)

**대상 파일**: `domain/event/MemberRegisteredEventListener.java`(신규), `application/service/SubscriptionService.java`

**의존성**: SUB-01, MEMBER-02(🟢 완료 — `MemberRegisteredEvent` 발행 완료됨)

**설계 확정 (2026-07-14, 사용자 승인)**:
- `MemberRegisteredEventListener`는 `@Component` + `@EventListener`로 `com.lcs.lxp.member.event.MemberRegisteredEvent`를 구독해 `subscriptionService.createSubscription(event.getMemberId(), 0L)`을 호출한다(0원 → `createSubscription`의 무료 분기가 이미 즉시 활성화까지 수행 — SUB-03에서 구현 완료).
- 범위 확장: SUB-05 준비 중 `ARCHITECTURE.md`의 "이벤트 리스너는 처리 시작 전후에 이벤트 정보 모두를 info 레벨로 로그에 기록한다" 규칙이 SUB-03의 `PaymentAdapter` 리스너 2개에 누락되어 있음을 발견(마스터가 SUB-03 완료 기준 구성 시 이 규칙을 빠뜨림). 사용자 확인 결과 이번 SUB-05에 번들로 포함해 `PaymentAdapter`도 함께 로깅을 보완하기로 함(SUB-03 재검증/무효화 절차 대신, 기능적 결함이 아닌 로깅 보완이므로 진행 기록에만 남김). 대상 파일에 `infrastructure/PaymentAdapter.java` 추가.

**완료 (2026-07-14)**:
- 테스트: `MemberRegisteredEventListenerTest.java` 신규(서로 다른 memberId 2건, `createSubscription(memberId, 0L)` 위임 검증).
- 구현: `MemberRegisteredEventListener` 신규(`@EventListener`로 `MemberRegisteredEvent` 구독, 처리 시작/종료 시 info 로그 기록 후 `createSubscription(memberId, 0L)` 호출). `PaymentAdapter`에 동일한 이벤트 리스너 로깅 규칙 보완(비즈니스 로직 변경 없음, 라인 단위 diff로 회귀 없음 확인).
- 리뷰: 승인(minor 1건 — 로깅 헬퍼 코드 중복, 기능 영향 없어 기록만).
- 테스트 실행: 전체 332/332 PASS(subscription 78개 포함), PMD 위반 0건, 전체 커버리지 89.4%, 타 BC(Member 67/Course 124/Security 63) 회귀 없음.
- 완료 근거: 리뷰 승인 + 테스트 332/332 통과 + PMD 0건 + 사용자 확인(2026-07-14).

---

## [SUB-06] 회원 정지/탈퇴 이벤트 리스너 (구독권 정지·환불 처리)

**설명**: `MemberSuspendedEvent`/`MemberWithdrawnEvent`를 구독해 활성화된 모든 구독권을 정지하고, 탈퇴 시에는 환불 정책을 검사하여 통과하면 환불까지 실시.

**완료 기준 (2026-07-15 갱신)**:
- 정지 이벤트 수신 시: 해당 회원의 활성화된(`isValid()` 또는 activatedAt != null && 미정지·미취소) 모든 구독권을 정지
- 탈퇴 이벤트 수신 시: 해당 회원의 활성화된 모든 구독권을 정지 + 환불 정책(SUB-04에서 구현할 판정 로직 재사용)을 검사하여 통과하면 환불 실시(`Payment(REFUND)` 생성·추가·저장 후 `RefundRequestedEvent` 발행)
- 취소(`cancel()`) 로직은 이 작업 범위에 포함되지 않는다(신규 규칙에서 삭제됨 — 탈퇴 시에는 정지만 하고 취소하지 않음)

**관련 규칙 위치**: `.claude/domain/SUBSCRIPTION.md` "구독권 서비스 비즈니스 규칙"(회원 정지/탈퇴 이벤트 처리, 환불 정책)

**대상 파일**: `domain/event/MemberSuspendedEventListener.java`(신규), `domain/event/MemberWithdrawnEventListener.java`(신규), `application/service/SubscriptionService.java`

**의존성**: SUB-01, SUB-04, MEMBER-05(🟢), MEMBER-04(🟢)

**진행 기록**: (미착수) — `TASK.md`의 "Subscription BC: MemberSuspendedEvent/MemberWithdrawnEvent 리스너" 자리표시자를 2026-07-13 계획에서 SUB-06으로 구체화함(치환 근거: MEMBER-05, MEMBER-04 모두 🟢로 완료되어 의존성 충족). "회원 취소 이벤트" 표현의 재확인 필요 사항은 상단 참고(2026-07-15 해소). 2026-07-15: `SUBSCRIPTION.md` 갱신에 따라 완료 기준 전면 개정(취소 로직 삭제, 환불 정책 검사 추가) + 의존성에 SUB-04 추가, 상태를 🟠(연관작업대기)로 조정(SUB-01·SUB-04 재검증 완료 후 착수 가능).

**설계 결정 (2026-07-15, 착수 시)**: "활성화된 모든 구독권 정지"와 "환불 정책 통과 시 환불"을 각각 독립적으로 구현하면, 환불 대상 구독권을 먼저 직접 `suspend()`한 뒤 환불 이벤트 처리(`PaymentAdapter`의 환불 성공 응답)가 또 `suspend()`를 호출해 SUB-01의 "중복 정지 금지" 불변식과 충돌한다(SUB-04에서 이미 겪은 것과 동일한 문제 패턴). 이를 피하기 위해 `processMemberWithdrawal`에서는 환불 정책을 만족하는 구독권(최대 1개, `isEligibleForRefund`의 "유료 구독권 정확히 1개" 조건상 자명)에 대해서는 직접 `suspend()`를 호출하지 않고 환불 이벤트만 발행하며, 나머지 활성 구독권만 직접 `suspend()`한다. 리뷰에서 이 설계가 실제로 지켜졌음을 코드 추적으로 확인함.

**완료 (2026-07-15)**:
- 테스트: `SubscriptionServiceTest.java`에 `suspendActiveSubscriptions`/`processMemberWithdrawal` 테스트 8종 추가(다수 활성 정지, 정지·취소·미활성 제외, 대상 없음, 환불정책 만족+타 활성 구독권 혼재, 전부 무료, 유료 2개 이상, 14일 경과, 활성 구독권 없음). `MemberSuspendedEventListenerTest.java`/`MemberWithdrawnEventListenerTest.java` 신규(각 2종, 이벤트→서비스 위임 검증).
- 구현: `MemberSuspendedEventListener`/`MemberWithdrawnEventListener` 신규(`MemberRegisteredEventListener`와 동일한 로깅 컨벤션). `SubscriptionService`에 `suspendActiveSubscriptions(memberId)`(활성 구독권 전부 정지), `processMemberWithdrawal(memberId)`(활성 구독권 중 환불 정책 만족 항목은 `requestRefund`로, 나머지는 직접 `suspend()`) 추가. 기존 `cancelSubscription`의 환불 처리 로직을 `requestRefund` private 메서드로 추출해 재사용(동작·시그니처 불변, SUB-04 테스트 회귀 없음 확인).
- 리뷰: 승인(PASS, minor 1건 — `processMemberWithdrawal`이 활성 구독권마다 `isEligibleForRefund` 내부에서 `findByMemberId`를 재호출하는 N+1 조회, 정확성에는 영향 없어 기록만).
- 테스트 실행: `SubscriptionServiceTest` 18/18, `MemberSuspendedEventListenerTest` 2/2, `MemberWithdrawnEventListenerTest` 2/2 PASS(전체 PASS), PMD 위반 0건, 전체 커버리지 89%.
- 완료 근거: 리뷰 승인 + 테스트 전체 통과 + PMD 0건 + 사용자 확인(2026-07-15).

---

## [SUB-07] 만료 임박 구독권 자동 재발급 배치

**설명**: 매일 0시 재발급 대상 구독권(만료 2일 전, 활성·비정지·비취소)을 조회해 자동 재발급하는 스케줄러 도입. 기존 수동 트리거 엔드포인트(`POST /api/subscriptions/reissue`)는 유지 여부 착수 시 결정.

**완료 기준**: `@Scheduled` cron으로 매일 0시 실행, `Subscription.isEligibleForReissue()` 대상 전체에 대해 `reissue()` 수행 및 결제 요청 흐름 연동

**관련 규칙 위치**: `.claude/domain/SUBSCRIPTION.md` "구독권 서비스 비즈니스 규칙"(매일 0시 재발급 배치)

**대상 파일**: `infrastructure/SubscriptionReissueScheduler.java`(신규), `application/service/SubscriptionService.java`

**의존성**: SUB-01, SUB-03

**진행 기록**: (미착수)
