# 구현 완료 목록

## 강좌 컨텐츠 관리 바운디드 컨텍스트

### 강좌·강의·미션 도메인 모델 (2026-06-22)

**구현 내용**
- `common/exception/DomainException` — 도메인 예외 기반 추상 클래스
- `course/exception/CourseException` — 강좌 도메인 예외
- `course/model/ContentStatus` — 공개(PUBLIC) / 비공개(PRIVATE) 상태 enum
- `course/model/Title` — 제목 VO (@Embeddable), 빈 값·100자 초과 불허
- `course/model/InstructorId`, `CourseId`, `LectureId`, `MissionId` — ID VO (record)
- `course/model/Course` — 강좌 애그리거트 루트
- `course/model/Lecture` — 강의 엔티티
- `course/model/Mission` — 미션 엔티티
- `course/repository/CourseRepository`, `LectureRepository`, `MissionRepository`

**핵심 불변식**
- 강좌·강의·미션은 비공개 상태로 생성됨
- 제목은 비어있을 수 없고 100자를 초과할 수 없음
- 강좌 공개 조건: 활성화된 강의·미션 각각 1개 이상 필요
- 공개 상태에서 강좌 수정·강의/미션 추가 불가
- 강좌가 비공개면 강의·미션의 상태와 무관하게 수정 허용
- 강좌가 공개면 강의·미션은 자신의 상태에 따라 수정 가능 여부 결정
- 비공개(unpublish) 처리는 soft delete와 동일 (deleted = true)

**테스트 현황**: TitleTest(6), CourseTest(12), LectureTest(8), MissionTest(8) — 전체 통과

### 강좌 서비스 (2026-06-22)

**구현 내용**
- `member/model/MemberRole` — 회원 유형 enum (MEMBER, INSTRUCTOR, ADMIN)
- `course/service/CourseService` — 강좌 애플리케이션 서비스

**핵심 역할**
- `createCourse`: 강사만 강좌를 생성할 수 있음, 생성 후 저장
- `updateCourse`: 강좌 제목 수정 (공개 상태 체크는 도메인 위임)
- `publishCourse`: 강사만 강좌를 공개할 수 있음
- `unpublishCourse`: 강사 또는 어드민만 강좌를 비공개할 수 있음
- 역할 조건 불충족 또는 강좌 미존재 시 `CourseException` 발생

**테스트 현황**: CourseServiceTest(14) — 전체 통과
