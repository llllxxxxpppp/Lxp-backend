# 강좌 후기 애그리거트 (Aggregate) & CRC

## 강좌 후기 관리 바운디드 컨텍스트

### 강좌 후기 애그리거트 (root)

- 구독자만 강좌 후기를 작성 가능함
- 강좌 후기는 작성자 본인만 수정/삭제 가능함
- 어드민은 강좌 후기를 삭제 가능함
- 해당 강좌의 미션 답안을 1개 이상 제출해야 작성할 수 있다.
- 구독자는 강좌 후기를 신고할 수 있다.

### CourseReview CRC

| Class | Responsibility | Collaborator |
| --- | --- | --- |
| CourseReviewService |   • 강좌 후기 생성을 위해 회원이 유효한 구독권을 소유하고 있는지 확인한다. |   • SubscriptionService |
|  |   • 강좌 후기를 생성하고 저장한다. |   • CourseReviewRepository |
|  |   • 강좌 후기 수정/삭제를 위해 강좌 후기를 작성했던 회원이 맞는지 확인한다. |   • MemberService |
|  |   • 강좌 신고 생성을 위해 회원이 유효한 구독권을 소유하고 있는지 확인한다. |   • SubscriptionService |
|  |   • 후기를 작성하려는 회원이 미션을 1개 이상 제출하였는지 확인한다. |   • CourseService |
| CourseReview (root aggregate) |   • 강좌 후기를 생성/수정/삭제한다. |   • CourseReviewId |
| CourseReviewId (VO) |   • 강좌 후기 Id를 표현한다. |  |
