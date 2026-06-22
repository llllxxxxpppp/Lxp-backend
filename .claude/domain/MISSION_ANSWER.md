# 미션 답안 관리 애그리거트 (Aggregate) & CRC

## 미션 답안 관리 바운디드 컨텍스트

### 미션 답안 애그리거트 (root)

- 미션 답안은 식별자를 가져야 한다.
- 미션 답안은 단독으로 생성 되거나 존재할 수 없다.
- 미션 답안은 빈칸으로 제출해서 안된다.
- 비공개 된 강좌에는 미션 답안을 작성할 수 없다.
- 미션의 유형은 서술형으로 하되 추후 확장한다.
- 제출된 답안 미션에 대해서 강사, 구독자만 신고 가능하다.
- 미션 답안은 제출한 회원이 삭제할 수 있다.
- 미션 답안은 어드민이 삭제할 수 있다.
- 미션 답안 좋아요는 제출된 답안에 대해서만 할 수 있다.
- 미션 답안이 삭제되면 미션 답안 댓글도 같이 삭제된다.

#### Mission Answer CRC

| **Class** | **Responsibility** | **Collaborator** |
| --- | --- | --- |
| MissionAnswerService
(애플리케이션 서비스) |   •  비공개 된 강좌인지 사전에 검증한다. | • MissionAnswerRepository
• MissionService
• SubscriptionService |
|  |   • 제출한 회원 본인 또는 어드민인지 권한을 확인한다.  |   • MemberService |
|  |   • 이미 제출된 답안인지 상태를 검증한다.  |   • MissionAnswerRepository |
|  |   • 미션이 존재하는지 확인한다. |   • MissionService |
|  |   • 미션 답안 신고 생성 전, 유효한 회원(강사, 구독자)의 요청인지 검사한다.  |   • MissionService
  • MemberService |
| MissionAnswer
(애그리거트) |   • 미션 답안 생성을 담당한다. |   • MissionAnswerId
  • MemberId |
|  |   • 미션 답안 내용이 빈칸인지 검증한다. 빈칸 제출을 방지한다.  |  |
|  |   • 미션 답안 삭제를 처리한다. |   • MemberId |
|  |   • 미션 답안 좋아요 처리를 담당한다.  |   • MemberId |
| MissionAnswerId (VO) |   • MissionAnserId를 표현한다. |  |

### 미션 답안 댓글 애그리거트

- 구독자, 강사만 미션 답안 댓글을 생성할 수 있다.
- 미션 댓글은 빈칸으로 제출해서 안된다.
- 미션 답안 댓글은 식별자를 가져야 한다.
- 미션 답안 댓글은 단독으로 생성 되거나 존재할 수 없다.
- 미션 답안 댓글은 강사, 어드민이 삭제할 수 있다.
- 미션 답안 댓글은 생성한 회원이 삭제할 수 있다.
- 미션 답안 댓글은 생성한 회원이 수정할 수 있다.
- 미션 답안 댓글은 구독자가 신고할 수 있다.

#### Mission Answer Comments CRC

| **Class** | **Responsibility** | **Collaborator** |
| --- | --- | --- |
| MissionAnswerComment
(애그리거트) |   • 미션 답안 댓글 생성을 담당한다. |   • MissionAnswerCommentsId
  • MissionAnswerId
  • MemberId |
|  |   • 미션 답안 댓글 수정한다. |   • MemberId |
|  |   • 미션 답안 댓글 삭제한다. |   • MemberId |
|  |   • 댓글 내용이 빈칸인지 검증한다.  |  |
| MissionAnswerCommentsId(VO) |   • 미션 답안 댓글 Id를 표현한다. |  |
