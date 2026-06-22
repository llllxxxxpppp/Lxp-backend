# 강좌 컨텐트 관리 애그리거트 (Aggregate) & CRC

## 강좌 컨텐츠 관리 바운디드 컨텍스트

### 강좌 애그리거트 (root)

- 강좌는 강사 권한의 회원만 생성할 수 있다.
- 강좌 제목은 비어있을 수 없다.
- 강좌 제목은 100자를 초과할 수 없다.
- 공개 상태에서는 수정할 수 없다.
- 강사, 시스템은 강좌를 공개할 수 있다.
- 강사, 어드민은 강좌를 비공개할 수 있다.
- 구독자는 강좌를 신고할 수 있다.
- 강좌는 비공개 상태로 생성된다.
- 강좌, 강의, 미션에 대한 임시 저장은 없다. (비공개로 저장할 것)
- 강좌는 강의와 미션을 1개 이상 포함해야 공개할 수 있다.
- 강좌 비공개는 soft delete 처리와 동일하다.
- 강좌 신고 시 어드민이 확인 후 강좌를 비공개 할 수 있다. (위와 중복됨)

#### Course CRC

| Class | Responsibility | Collaborator |
| --- | --- | --- |
| CourseService (애플리케이션 서비스) |   • 강좌 생성 요청을 처리한다. |   • CourseRepository
  • Course |
|  |   • 강좌 수정 요청을 처리한다. |   • CourseRepository
  • Course |
|  |   • 강좌 비공개 처리한다. |   • CourseRepository
  • Course |
|  |   • 비공개 처리를 위한 요청을 받으면 강사와 어드민인지 확인 한다. |   • CourseRepository
  • Course |
|  |   **•** 강사의 강좌 업로드 요청을 처리한다. |   • CourseRepository
  • Course |
|  |   • 공개 요청을 강사가 한건지 확인한다. |   • CourseRepository
  • Course |
| Course (애그리거트 루트) |   • 강좌 생성을 담당한다. |   • CourseId
  • InstructorId
  • Title
  • Status |
|  |   • 강좌 정보를 수정한다. |   • CourseId
  •Status |
|  |   • 강좌가 비공개 되어있으면 강의와 미션의 수정을 허용한다. |   •Status |
|  |   • 강좌가 공개되어 있으면 강의와 미션의 수정은 각각의 애그리거트의 공개/비공새 설정을 따른다. |   • Status |
|  |   • 강좌 정보 수정 전 공개/비공개를 보고 수정 가능한지 검사한다. |   • Status |
|  |   • 강좌 공개/비공개한다. |   • Status |
|  |   • 강좌 공개 전 강의와 미션 요건을 검사한다. |   • Status
  • Lecture
  • Mission |
|  |   ◦ 강의와 미션을 1개 이상 포함해야 공개 가능 |   • Status
  • Lecture
  • Mission |
| Title(VO) |   • 강좌 제목이 유효한지 검증
  • 비어 있을 수 없다
  • 100자 이상 초과면 불가 |  |
| Status(VO) |   • 공개(PUBLIC) / 비공개(PRIVATE) 상태를 표현한다. |  |
| InstructorId(VO) |   • 강사(회원)를 식별하는 ID 값을 담는다. |  |
| CourseId (VO) |   • 강좌 Id를 표현한다. |  |

### 강의 애그리거트

- 강의는 강사 권한의 회원만 생성할 수 있다.
- 강의 제목은 비어있을 수 없다.
- 제목은 100자를 초과할 수 없다.
- 공개 상태에서는 수정할 수 없다.
- 강사는 강의를 공개할 수 있다.
- 강사, 어드민은 강의를 비공개할 수 있다.
- 강의 비공개는 soft delete 처리와 동일하다.

#### Lecture CRC

| Class | Responsibility | Collaborator |
| --- | --- | --- |
| Lecture (애그리거트) |   • 강의 생성을 담당한다. |   • LectureId
  • Title
  • Status |
|  |   • 강의 제목 수정한다. |   • Title |
|  |   • 강의 정보를 수정한다. |   • LectureId |
|  |   ◦ 공개 상태에서는 수정할 수 없다. |   • Status |
|  |   • 강의를 공개 / 비공개 한다. |   • Status |
| LectureId (VO) |   • 강의 id를 표현한다. |  |

### 미션 애그리거트

- 미션은 강사 권한의 회원만 생성할 수 있다.
- 미션 제목은 비어있을 수 없다.
- 제목은 100자를 초과할 수 없다.
- 공개 상태에서는 수정할 수 없다.
- 강사는 미션을 공개할 수 있다.
- 강사, 어드민은 미션을 비공개할 수 있다.
- 구독자가 미션 신고를 할 수 있다.
- 미션 비공개는 soft delete 처리와 동일하다.

#### Mission CRC

| Class | Responsibility | Collaborator |
| --- | --- | --- |
| Mission (애그리거트) |   • 미션 생성을 담당한다. |   • MissionId
  • Title |
|  |   • 미션 제목을 수정한다. |   • Title |
|  |   • 미션 정보를 수정한다. |   • MissionId |
|  |   ◦ 공개 상태에서는 수정할 수 없다. |   • Status |
|  |   • 미션을 공개 / 비공개 한다. |   • Status |
| MissionId(VO) |   • 미션 id를 표현한다. |  |
