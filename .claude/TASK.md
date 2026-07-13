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

**의존성 순서 (선행 → 후행):** Member → Subscription, Course → MissionAnswer → CourseReview

### Member 도메인 관련 확정 사항 (2026-07-13, 사용자 확정)

- `.claude/domain/MEMBER_temp.md`는 참조에서 제외한다. 정식 도메인 정의는 `.claude/domain/MEMBER.md`만을 근거로 한다.
  - 근거: `MEMBER.md`는 최초 작성(6a4392d) 시점부터 현재까지 "강사 유형의 회원은 어드민만 생성 가능하다"를 일관되게 유지하고 있어 자체 모순이 없음. 모순은 `MEMBER_temp.md`(별도 메모)에만 존재했음.
- 강사 자진 탈퇴 기능은 이번 범위에 **미포함** — 강사는 정지만 가능(현행 유지).
- 정지/탈퇴 회원 자료 2년 보존 정책은 이번 범위에 **미포함** — 추후 별도 진행.

## 작업 요약표

| ID | 상태 | 작업명 | 도메인 | 담당 | 의존성 |
|---|---|---|---|---|---|
| MEMBER-01 | 🟢 | 회원 도메인 모델 (SINGLE_TABLE 상속, 검증, 타임스탬프) | Member | - | 없음 |
| MEMBER-02 | 🟡 | 회원가입/로그인/로그아웃 (JWT) | Member | - | MEMBER-01 |
| MEMBER-03 | 🟢 | 어드민 전용 강사 생성/정지 API | Member | - | MEMBER-01, MEMBER-02 |
| MEMBER-04 | 🟢 | 회원 자기 관리 (비밀번호 변경/강사 프로필 수정/자진 탈퇴) | Member | - | MEMBER-01, MEMBER-02 |
| MEMBER-05 | 🟢 | 회원 정지 처리 + 이벤트 발행 | Member | - | MEMBER-01 |
| MEMBER-06 | 🟢 | 탈퇴 회원 이메일 마스킹 | Member | - | MEMBER-04 |

세부 내용은 `.claude/task/task-member.md` 참고.

| COURSE-01 | ⚪ | 강좌/강의/미션 도메인 모델 (VO·엔티티·초기 상태) | Course | - | 없음 |
| COURSE-02 | 🟢 | 강좌 생성/수정/조회 API | Course | - | COURSE-01 |
| COURSE-03 | 🟢 | 강좌 목록 페이지네이션·검색 | Course | - | COURSE-02 |
| COURSE-04 | 🟢 | 강의/미션 추가 및 공개·비공개 API | Course | - | COURSE-01 |
| COURSE-05 | ⚪ | 강의 자료 타입(확장자) 필드 도입 | Course | - | COURSE-01 |
| COURSE-06a | ⚪ | 강좌/강의/미션 soft delete 모델 | Course | - | COURSE-01 |
| COURSE-06b | ⚪ | 강좌/강의/미션 삭제 API | Course | - | COURSE-06a |
| COURSE-07a | ⚪ | Sortable 인터페이스 + 순번 필드 | Course | - | COURSE-01 |
| COURSE-07b | ⚪ | 강좌 단위 순서 변경 API | Course | - | COURSE-07a |
| COURSE-08a | ⚪ | InstructorSuspendedEvent 리스너(정지 강사 강좌 비공개) | Course | - | MEMBER-03 |
| COURSE-08b | ⚪ | 정지된 강사 2차 방어 인터셉터 | Course | - | COURSE-08a |

세부 내용은 `.claude/task/task-course.md` 참고.

| SUB-01 | ⚪ | 구독권 애그리거트 재설계 (상태 표현·재발급 체인·유효기간) | Subscription | - | 없음 |
| SUB-02 | 🟠 | Payment 애그리거트 요청 리스트 구조 재설계 | Subscription | - | SUB-01 |
| SUB-03 | 🟠 | 결제/환불 이벤트 기반 아키텍처 전환 | Subscription | - | SUB-02 |
| SUB-04 | 🟠 | 구독권 생성/조회/취소 API 정합성 조정 (가격 19,800원) | Subscription | - | SUB-01, SUB-02 |
| SUB-05 | 🟠 | 회원가입 이벤트 리스너(무료 구독권 자동 발급) | Subscription | - | SUB-01, MEMBER-02 |
| SUB-06 | 🟠 | 회원 정지/탈퇴 이벤트 리스너(구독권 정지·취소) | Subscription | - | SUB-01, MEMBER-05, MEMBER-04 |
| SUB-07 | 🟠 | 만료 임박 구독권 자동 재발급 배치 | Subscription | - | SUB-01, SUB-03 |

세부 내용은 `.claude/task/task-subscription.md` 참고.

### Member 관련 🟠 대기 항목 (타 BC 계획 시 등록 예정, 지금 임의 생성하지 않음)

- ~~Subscription BC: `MemberSuspendedEvent`/`MemberWithdrawnEvent` 리스너~~ → 2026-07-13 Subscription BC 계획 수립 시 SUB-06으로 구체화(자리표시자 치환 완료).
- ~~Course BC: `InstructorSuspendedEvent` 리스너 + 정지 강사 2차 방어 인터셉터~~ → 2026-07-13 Course BC 계획 수립 시 COURSE-08a/08b로 구체화(자리표시자 치환 완료).

### 도메인별 개요

- **회원 관리 (Member)**: 회원 가입/인증/정지/탈퇴, 강사 프로필, 어드민 강사 관리. 기존 구현 내역은 `.claude/PROGRESS.md`(레거시, 대조 전용) 참고.
- **강좌 컨텐츠 관리 (Course)**: 강좌/강의/미션 CRUD 및 공개·비공개는 기존 구현 존재(대부분 🟢). soft delete, 순서(Sortable) 관리, 강의 자료 타입, 정지 강사 방어는 신규 구현 필요(⚪). COURSE-01은 강의/미션 초기 생성 상태를 도메인 문서 기준(PUBLIC)으로 맞추는 수정이 필요해 ⚪로 시작.
- **구독권 결제 관리 (Subscription)**: 기존 구현(2026-06-23)이 도메인 문서와 상태 표현·재발급 이력·유효기간 계산·가격·결제 아키텍처에서 크게 달라 전면 재설계로 확정(2026-07-13 사용자 확정). 기존 코드 기반 🟢 판정 항목 없음 — SUB-01부터 순차 진행.
