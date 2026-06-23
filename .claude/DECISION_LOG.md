# 구현 판단 근거 기록

설계 및 구현 과정에서 내린 판단과 그 근거를 기록한다.
모호한 명세, 트레이드오프, 의도적인 단순화 등을 남긴다.

---

## 설계 단계 (2026-06-23)

### [설계-001] Payment를 Subscription과 별도 애그리거트로 분리

**판단:** `Payment`를 `Subscription`에 내포시키지 않고 별도 `@Entity` + `PaymentRepository`로 분리한다.

**근거:**
- SUBSCRIPTION.md CRC에서 `PaymentAdapter`의 협력자로 `PaymentRepository`가 명시됨.
  `PaymentAdapter`가 `Payment` 상태를 직접 갱신하려면 독립적인 Repository가 필요.
- `Subscription`과 `Payment`는 생명주기가 다름: Payment는 결제 결과가 확정되면 불변에 가깝고,
  Subscription은 이후에도 상태 전이(정지, 취소, 재발급)가 계속 발생.

---

### [설계-002] 결제를 동기 더미로 처리

**판단:** `PaymentAdapter`가 `DummyPaymentGateway`를 동기로 호출하여 즉시 성공/실패 결과를 반환한다.
별도 웹훅 콜백 엔드포인트를 두지 않는다.

**근거:**
- SUBSCRIPTION.md에서 "외부 결제 시스템에 대해서는 실제 연결하지 않고 dummy 객체를 만들어 시뮬레이션"이라고 명시.
- 비동기 웹훅 방식은 테스트 복잡도가 크게 증가하고, 더미 목적에 과함.
- 동기 처리 시 `createSubscription` 한 번의 트랜잭션 내에서 구독권 활성화까지 완결할 수 있음.

---

### [설계-003] PaymentStatus에 REFUND_REQUEST_FAILED 추가

**판단:** SUBSCRIPTION.md 원문의 `환불 요청 실패(REFUND_FAILED)`와 `환불 실패(REFUND_FAILED)` 중복을
`REFUND_REQUEST_FAILED`(요청 자체 실패)와 `REFUND_FAILED`(대행사 거부)로 구분한다.

**근거:**
- 원문이 두 항목 모두 `REFUND_FAILED`로 표기한 것은 명세 오류로 판단.
- "환불 요청 실패"는 대행사에 요청을 보내기 전 실패(네트워크, 유효성 등),
  "환불 실패"는 요청은 성공했으나 대행사가 거부한 경우로 의미가 다름.
- 두 케이스를 구분해야 재시도 로직이나 운영 대응이 가능.

---

### [설계-004] '첫 구독권 2주 이내 취소' 환불 조건 해석

**판단:** "첫 구독권"을 "해당 회원의 최초 구독권"으로 해석한다.
`SubscriptionRepository.existsByMemberIdAndIdNot(memberId, subscriptionId)`로 이전 구독 이력 확인.

**근거:**
- LANGUAGE.md 구독권 섹션: "구독권 취소 = 구독권의 자동 갱신을 취소하는 행위".
  재발급은 자동 갱신이므로 재발급된 구독권 취소는 환불 대상이 아님.
- LANGUAGE.md: "회원 가입 시 1개월의 구독권을 주어 무료 체험 기간 부여" →
  최초 구독은 무료이므로 실제 환불 시나리오는 유료 첫 결제(무료 체험 이후 첫 결제) 취소에 해당.
- 재발급 구독권은 `DISCARDED` 이전 구독 이력이 존재하므로 "첫 구독" 조건 불충족.

---

### [설계-005] 재발급 엔드포인트를 수동 트리거(관리자용 API)로 구현

**판단:** 스케줄러 대신 `POST /api/subscriptions/reissue` 관리자 API로 재발급을 트리거한다.

**근거:**
- Spring `@Scheduled`는 단위 테스트가 어렵고, 현재 프로젝트에 스케줄러 관련 설정이 없음.
- 더미 시스템이므로 "만료 1 영업일 전" 조건을 API 호출로 시뮬레이션하는 것이 검증에 유리.
- 추후 스케줄러로 전환 시 서비스 메서드(`reissueExpiring()`)는 그대로 재사용 가능.

---

---

## 구현 단계 (2026-06-23)

### [설계-006] PaymentAdapter가 SubscriptionService를 역호출하지 않음

**판단:** CRC상 PaymentAdapter가 SubscriptionService에 결과를 전달하도록 명세되어 있으나,
`PaymentAdapter`는 결과(PaymentResult)를 반환하고 `SubscriptionService`가 직접 Subscription을 갱신한다.

**근거:**
- CRC 그대로 구현 시 `SubscriptionService → PaymentAdapter → SubscriptionService` 순환 의존성 발생.
- Spring에서 순환 의존성은 빈 생성 실패를 야기.
- 결과 객체 반환 방식은 동일한 흐름을 유지하면서 의존성 방향을 단방향으로 정리.

---

### [구현-001] PaymentInfo는 불변 요청 정보(멱등키 + 금액)만 저장

**판단:** CRC의 "결제 상태를 저장한다" 항목에도 불구하고, `PaymentInfo`는 `idempotencyKey`와 `amount`만 가진 불변 @Embeddable로 구현한다. 결제 상태와 타임스탬프는 `Payment` 엔티티 필드로 직접 관리한다.

**근거:**
- @Embeddable을 상태 변이에 사용하면 VO 불변 원칙에 위배됨.
- 기존 코드베이스의 `Title` @Embeddable도 불변. 일관성 유지.
- 결제 상태(`PaymentStatus`)는 Payment 엔티티가 직접 관리하는 것이 ORM 관점에서도 자연스러움.

---

### [구현-002] Subscription.markPaymentFailed()는 매개변수 없음

**판단:** `Subscription`의 결제 실패 처리 메서드는 `PaymentFailureResponse`를 매개변수로 받지 않는다.

**근거:**
- CRC상 Subscription은 결제 실패 일시를 "기록"하지만, 실제 기록 위치는 `Payment` 엔티티.
- 매개변수로 받아 사용하지 않으면 PMD `UnusedFormalParameter` 위반.
- Subscription은 상태만 `PAYMENT_FAILED`로 전이하면 충분.

---

### [구현-003] 구독권 무료 여부는 서버가 결정 (요청 바디 amount 없음)

**판단:** `POST /api/subscriptions` 요청 바디에 금액 필드를 두지 않는다. 서버가 기존 구독 이력 유무로 무료/유료를 결정한다.

**근거:**
- LANGUAGE.md: "회원 가입 시 1개월의 구독권을 주어 무료 체험 기간을 부여함" → 첫 구독은 항상 무료.
- 금액을 클라이언트가 전달하면 조작 가능성 있음.
- `subscriptionRepository.existsByMemberId(memberId)` 결과: false → amount=0(무료), true → MONTHLY_SUBSCRIPTION_PRICE(유료).

---

### [구현-004] 환불 조건을 단순화 (첫 구독 여부 제외)

**판단:** 환불 조건을 "활성화 후 14일 이내 AND 유료 구독"으로 단순화한다. "첫 구독권"인지 여부는 검사하지 않는다.

**근거:**
- "첫 구독권" 검사는 `Subscription`과 `Payment`를 조인해야 하는 복잡한 쿼리가 필요.
- 무료 체험(amount=0)은 `payment.isFree()` 체크로 환불에서 제외되므로 사실상 첫 유료 결제만 해당됨.
- 재발급 구독권은 expiresAt이 31일이므로, 활성화 후 14일 이내 취소가 현실적으로 재발급 직후에 해당.

---

### [구현-005] 재발급 금액은 MONTHLY_SUBSCRIPTION_PRICE 상수 사용

**판단:** 재발급 시 결제 금액은 이전 결제 금액을 조회하지 않고 `MONTHLY_SUBSCRIPTION_PRICE = 9900` 상수를 사용한다.

**근거:**
- 도메인 문서에 구독 가격이 명시되어 있지 않음.
- 이전 결제가 무료(amount=0)인 경우 그대로 승계하면 재발급도 무료가 되는 문제 발생.
- 재발급 = 무료 체험 만료 후 유료 전환이므로 고정 가격이 합리적.

---

### [구현-006] '1 영업일 전' = 1 캘린더일로 단순 처리

**판단:** "만료 1 영업일 전" 계산 시 실제 영업일 계산 없이 `now + 1일` 이내를 조건으로 사용한다.

**근거:**
- 영업일 계산은 공휴일 API 연동이 필요하여 현재 더미 시스템 범위를 벗어남.
- 실제 운영 시에는 스케줄러 실행 시점을 영업일에만 동작하도록 설정하면 동일 효과.

---

### [구현-007] Payment 타임스탬프를 occurredAt 단일 필드로 통일

**판단:** `paymentRequestedAt`, `paymentSucceededAt`, `paymentFailedAt`, `refundRequestedAt` 등 6개 타임스탬프 필드를 `occurredAt` 하나로 대체한다.

**근거:**
- 각 상태(enum)가 이미 "현재 어떤 상태인지"를 표현하므로, 타임스탬프도 "이 상태가 된 시각" 하나면 충분.
- 6개 컬럼 → 1개 컬럼으로 DB 스키마 단순화.
- 구독권 도메인에서는 상태 변이 이력보다 현재 상태 시각이 중요 (이력이 필요하면 별도 이력 테이블이 맞음).

---

<!-- 구현 진행 중 판단이 추가될 때마다 아래에 이어서 기록 -->
