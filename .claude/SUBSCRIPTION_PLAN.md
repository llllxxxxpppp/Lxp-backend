# 구독권 결제 관리 구현 계획

## 패키지 구조

```
subscription/
├─ domain/
│   ├─ exception/SubscriptionException
│   ├─ model/
│   │   ├─ entity/
│   │   │   ├─ Subscription        ← 애그리거트 루트
│   │   │   └─ Payment             ← 별도 애그리거트
│   │   └─ vo/
│   │       ├─ SubscriptionId
│   │       ├─ SubscriptionStatus
│   │       ├─ PaymentId
│   │       ├─ PaymentInfo         (@Embeddable)
│   │       ├─ PaymentStatus
│   │       ├─ PaymentSuccessResponse
│   │       ├─ PaymentFailureResponse
│   │       ├─ RefundInfo
│   │       ├─ RefundSuccessResponse
│   │       └─ RefundFailureResponse
│   └─ repository/
│       ├─ SubscriptionRepository
│       └─ PaymentRepository
├─ application/
│   ├─ dto/
│   │   ├─ request/CreateSubscriptionRequest
│   │   └─ response/SubscriptionResponse
│   └─ service/SubscriptionService
├─ infrastructure/
│   ├─ PaymentGateway              ← 외부 대행사 포트 (인터페이스)
│   ├─ DummyPaymentGateway        ← 더미 구현체
│   └─ PaymentAdapter             ← 결제 흐름 조율
└─ presentation/
    ├─ SubscriptionController
    └─ SubscriptionExceptionHandler
```

## API 엔드포인트

| Method | Path | 설명 | 응답 |
|---|---|---|---|
| POST | `/api/subscriptions` | 구독권 생성 (결제 포함) | 201 |
| POST | `/api/subscriptions/{id}/cancel` | 구독권 취소 (환불 자동 판단) | 200 |
| GET | `/api/subscriptions/{id}` | 구독권 조회 | 200 |
| POST | `/api/subscriptions/reissue` | 만료 임박 구독권 재발급 (관리자용) | 200 |

## 핵심 불변식 → 구현 매핑

| 불변식 | 구현 위치 |
|---|---|
| 구독권은 비활성 상태로 생성 | `Subscription.create()` → `INACTIVE` |
| 무료면 즉시 활성화, 아니면 결제 요청 | `Subscription.create()` + `PaymentAdapter` |
| 유효기간 31일 | `expiresAt = createdAt + 31days` |
| 효력: ACTIVE && 만료 전 | `Subscription.isValid()` |
| 취소는 언제든 가능 | `Subscription.cancel()` |
| 첫 구독권 2주 이내 취소 → 전액 환불 | `SubscriptionService.cancel()` → `PaymentAdapter.requestRefund()` |
| 회원 정지 → 구독권 정지 | `Subscription.suspend()` |
| 만료 1 영업일 전 → 재발급 | `SubscriptionService.reissueExpiring()` |
| 재발급: 기존 폐기 후 신규 생성 | `Subscription.discard()` + `Subscription.create()` |
| 결제 성공 → 구독권 활성화 | `Subscription.activateByPayment()` + `Payment.handleSuccess()` |
| 결제 실패 → 실패 상태 기록 | `Subscription.handlePaymentFailure()` + `Payment.handleFailure()` |

## SubscriptionStatus 상태값

- `INACTIVE` — 비활성화 (생성 초기)
- `ACTIVE` — 활성화 (결제 성공 또는 무료)
- `CANCELLED` — 취소됨 (회원 자발적 취소)
- `SUSPENDED` — 정지됨 (회원 정지 연동)
- `PAYMENT_FAILED` — 결제 실패
- `DISCARDED` — 폐기됨 (재발급 시 기존 구독권)

## PaymentStatus 상태값

- `PAYMENT_NOT_REQUESTED` — 결제 요청 전
- `PAYMENT_REQUESTED` — 결제 요청됨
- `PAYMENT_REQUEST_FAILED` — 결제 요청 실패
- `PAYMENT_SUCCESS` — 결제 성공
- `PAYMENT_FAILED` — 결제 실패
- `REFUND_REQUESTED` — 환불 요청됨
- `REFUND_REQUEST_FAILED` — 환불 요청 실패
- `REFUND_SUCCESS` — 환불 성공
- `REFUND_FAILED` — 환불 실패

## 결제 흐름 (더미 동기 처리)

```
createSubscription(memberId, amount)
  → Subscription(INACTIVE) 저장
  → Payment 생성
  → [amount == 0] Subscription.activate() → ACTIVE
  → [amount > 0]  PaymentAdapter.requestPayment()
       → DummyPaymentGateway (항상 성공)
       → Payment.handleSuccess()
       → Subscription.activateByPayment() → ACTIVE
```

## 구현 진행 현황

| 단계 | 대상 | 상태 | 완료일 |
|---|---|---|---|
| 1 | `SubscriptionException` | ✅ 완료 | 2026-06-23 |
| 2 | 도메인 VO (`SubscriptionStatus`, `PaymentStatus`, `PaymentInfo`, 응답 VO들) | ✅ 완료 | 2026-06-23 |
| 3 | 도메인 엔티티 `Payment` + 테스트 (15개) | ✅ 완료 | 2026-06-23 |
| 4 | 도메인 엔티티 `Subscription` + 테스트 (18개) | ✅ 완료 | 2026-06-23 |
| 5 | `SubscriptionRepository`, `PaymentRepository` | ✅ 완료 | 2026-06-23 |
| 6 | `PaymentGateway` 인터페이스 + `DummyPaymentGateway` + `PaymentAdapter` | ✅ 완료 | 2026-06-23 |
| 7 | `SubscriptionService` + 테스트 (12개) | ✅ 완료 | 2026-06-23 |
| 8 | `SubscriptionController` + `SubscriptionExceptionHandler` + 테스트 (8개) | ✅ 완료 | 2026-06-23 |
