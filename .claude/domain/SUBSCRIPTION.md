# 구독권 애그리거트 (Aggregate) & CRC

## 구독권 결제 관리 바운디드 컨텍스트

### 구독권 애그리거트 (root)

- 회원이 첫 구독권 결제 2주 이내에 취소하는 경우에는 전액 환불한다.
- 구독권의 유효기간은 31일이다.
- 구독권은 결제가 성공되면 생성된다.
- 구독권 재발급은 유효기간을 31일 단위로 재발급한다.
- 구독권 재발급은 기존 구독권 폐기 후 재발급하는 형태로 한다.
- 만료되지 않은 구독권을 소요하고 있는 회원은 모든 서비스가 제공하는 모든 기능을 이용할 수 있다.
- 구독권은 활성화가 되어있고 유효기간이 지나지 않았을 때 효력이 있다.
- 회원이 정지된 경우 구독권을 정지시킨다.
- 회원은 언제든지 구독권을 취소할 수 있다.
- 취소 또는 정지되지 않은 구독권은 만료 1 영업일 전 재발급 요청된다.
- 재발급이 요청되면 새 구독권을 생성된다.
- 구독권은 비활성화 상태로 생성된다.
- 구독권이 생성되면
    - 무료 구독권이면, 구독권을 활성화한다.
    - 무료 구독권이 아니면, 결제를 요청한다.

#### Subscription CRC

| Class | Responsibility | Collaborator |
| --- | --- | --- |
| SubscriptionService
(애플리케이션 서비스) |   • 유효한 회원이 요청했는지 확인한다. |   • MemberService |
|  |   • 결제 성공 응답을 받아 구독권 활성화를 위임한다. |   • SubscriptionRepository
  • Subscription |
|  |   • 결제 실패 응답을 받아 구독권을 결제 실패 상태로 반영한다. |   • SubscriptionRepository
  • Subscription |
|  |   • 구독권 취소 요청 시 환불 필요 여부를 판단하여 환불을 요청한다. |   • SubscriptionRepository
  • PaymentService |
|  |   • 회원 정지 이벤트를 받아 구독권 정지를 위임한다. |   • SubscriptionRepository
  • MemberService |
|  |   • 만료 1 영업일 전 재발급 대상 구독권을 조회하고 재발급 한다. |   • SubscriptionRepository
  • Subscription |
|  |   • 재발급 시 기존 구독권을 폐기 상태로 전이한다. |   • Subscription |
| Subscription
(애그리거트 루트) |   • 구독권을 비활성 상태로 생성한다. |   • SubscriptionId |
|  |   • 무료 구독권이면 즉시 활성화한다. |  |
|  |   • 현재 시점 기준 구독권이 유효한지 판단한다. |  |
|  |   • 구독권을 취소한다. |  |
|  |   • 구독권을 정지한다. |  |
|  |   • 재발급 가능 여부를 판단한다. |  |
|  |   • 결제 성공 응답 시 결제 성공 일시와 상태를 기록하고 구독권을 활성화한다. |   • Payment |
|  |   • 결제 실패 응답 시 결제 실패 일시와 상태를 기록한다. |   • Payment |
| SubscriptionId (VO) |   • 구독권 id를 표현한다. |  |

### 결제 애그리거트

- 성공적으로 결제가 완료되면 구독권을 활성화한다.
- 결제가 요청되면 결제 대행사에 결제를 요청한다.

#### Payment CRC

| Class | Responsibility | Collaborator |
| --- | --- | --- |
| Payment (애그리게이트) |   • 구독권 결제를 위한 결제 요청을 생성한다. |   • PaymentId
  • PaymentInfo
  • PaymentSuccessResponse
  • PaymentFailureResponse
  • RefundInfo
  • RefundSuccessResponse
  • RefundFailureResponse
  • PaymentStatus |
|  |   • 결제 요청 상태를 관리한다. |  |
|  |   • 결제 승인 결과를 영구 저장한다. |  |
|  |   • 결제 거부 결과를 영구 저장한다. |  |
|  |   • 이미 승인/거부된 결제의 중복 처리를 방지한다. |  |
|  |   • 환불 요청을 생성한다. |   • Refund |
|  |   • 환불 성공 결과를 반영한다. |   • Refund |
|  |   • 환불 거부 결과를 반영한다. |   • Refund |
|  |   • 이미 환불된 결제의 중복 환불을 방지한다. |   • Refund |
| PaymentId (VO) |   • 결제 Id를 표현한다. |  |
| PaymentInfo (VO) |   • 결제 요청 정보를 생성한다. |   • PaymentId |
|  |   • 멱등키가 비어있지 않은지 확인한다. |  |
|  |   • 결제 일시를 저장한다. |  |
|  |   • 결제 금액을 저장한다. |  |
|  |   • 결제 상태를 저장한다 |  |
| PaymentStatus (VO) |   • 결제 상태를 표현한다.
      ◦ 결제 요청 전 (PAYMENT_NOT_REQUESTED)
      ◦ 결제 요청 실패 (PAYMENT_REQUEST_FAILED)
      ◦ 결제 요청됨 (PAYMENT_REQUESTED)
      ◦ 결제 성공 (PAYMENT_SUCCESS)
      ◦ 결제 실패 (PAYMENT_FAILED)
  • 환불 상태를 표현한다.
      ◦ 환불 요청 실패 (REFUND_FAILED)
      ◦ 환불 요청됨 (REFUND_REQUESTED)
      ◦ 환불 성공 (REFUND_SUCCESS)
      ◦ 환불 실패 (REFUND_FAILED) |  |
| PaymentSuccessResponse (VO) |   • 결제 성공 응답을 표현한다. |   • PaymentId |
| PaymentFailureResponse (VO) |   • 결제 실패 응답을 표현한다. |   • PaymentId |
| RefundInfo (VO) |   • 환불 요청 정보를 표현한다. |   • PaymentId |
| RefundSuccessResponse (VO) |   • 환불 성공 결과를 기록한다. |   • PaymentId |
| RefundFailureResponse (VO) |   • 환불 실패 결과를 기록한다. |   • PaymentId |
| PaymentAdapter |   • 외부 결제 시스템에 결제를 요청한다. |   • PaymentService |
|  |   • 결제 승인 응답을 받아 결제 상태를 변경한다. |   • PaymentRepository
  • PaymentService |
|  |   • 결제 거부 응답을 받아 결제 상태를 변경한다. |   • PaymentRepository
  • PaymentService |
|  |   • 결제 성공 결과를 구독권으로 전달한다. |   • SubscriptionService |
|  |   • 결제 실패 결과를 구독권으로 전달한다. |   • SubscriptionService |
|  |   • 환불 요청을 한다. |   • PaymentService
  • Refund |
|  |   • 환불 성공/실패 응답을 받아 결제 상태를 변경한다. |   • PaymentRepository
  • Refund |
|  |   • 결제 대행사에 결제 요청을 전달한다. |   • PaymentService |
|  |   • 결제 대행사 응답을 결제 결과로 변환한다. |   • PaymentService |
|  |   • 결제 대행사에 환불 요청을 전달한다. |   • PaymentService
  • Refund |
