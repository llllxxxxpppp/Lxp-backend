# task-member.md

회원 관리(Member) 바운디드 컨텍스트 세부 작업 파일이다. 상태의 원본은 `.claude/TASK.md` 요약표이며, 본 파일에는 상태를 중복 기록하지 않는다.

도출 근거: `.claude/domain/MEMBER.md` (정식 문서만 참조, `MEMBER_temp.md` 제외) + `.claude/ARCHITECTURE.md`.
대조 근거: `.claude/PROGRESS.md` 회원 관리 바운디드 컨텍스트 섹션 + 실제 코드 확인(2026-07-13, 이번 세션).

---

## [MEMBER-01] 회원 도메인 모델

**설명**: 회원 유형(일반/강사/어드민) SINGLE_TABLE 상속 구조, `MemberId` VO, 이메일/비밀번호 유효성 검증, 생성/수정일시(OffsetDateTime) 관리.

**완료 기준**:
- 이메일 blank 및 형식(정규식) 검증 시 예외 발생
- 비밀번호 blank 검증 시 예외 발생
- `createdAt`은 생성 시 not-null, `updatedAt`은 생성 시 null이며 `touch()` 호출 시에만 갱신
- `MemberId`는 record VO

**관련 규칙 위치**: `.claude/domain/MEMBER.md` "도메인 모델 규칙 > 공통" / `.claude/rules/implementation-rules.md` (OffsetDateTime, 수정일자 null 초기화)

**대상 파일**: `Member.java`, `RegularMember.java`, `InstructorMember.java`, `AdminMember.java`, `model/vo/MemberId.java`

**진행 기록**:
- 근거: `Member.java` L25-26(EMAIL_PATTERN), L121-134(validateEmail/validatePassword), L44-48(createdAt not-null/updatable=false, updatedAt nullable), L117-119(touch()). `PROGRESS.md` "회원 도메인 모델(2026-06-24)" 기록과 일치.
- 테스트: `MemberTest`(31케이스), `InstructorProfileTest`(6케이스) 확인됨.
- 판정: 🟢 (코드 + 테스트 존재 확인, 2026-07-13)

---

## [MEMBER-02] 회원가입/로그인/로그아웃 (JWT)

**설명**: 일반 회원 가입, 이메일 중복 검사, 로그인 시 JWT 액세스/리프레시 토큰 발급, 로그아웃 시 리프레시 토큰 삭제. 회원가입 시 `MemberRegisteredEvent`(회원 id 포함) 발행 추가(2026-07-13 계획 변경, 아래 참고).

**완료 기준**:
- 이메일 중복 시 가입 거부
- 로그인 성공 시 액세스/리프레시 토큰 반환
- 로그아웃 시 저장된 리프레시 토큰 삭제
- 회원가입 성공 시 `MemberRegisteredEvent`(회원 id 포함) 발행

**관련 규칙 위치**: `.claude/domain/MEMBER.md` "서비스 비즈니스 규칙 > 공통" (이메일 중복 불가), "서비스 비즈니스 규칙 > 일반 회원" (회원가입 이벤트 발행), "이벤트 규칙 > 회원 가입 이벤트", `.claude/CLAUDE.md` 사용 기술(Spring Security + JJWT)

**대상 파일**: `MemberController.java`, `MemberService.java`(login/register/logout), `dto/request/SignupRequestDTO.java`, `dto/request/LoginRequestDTO.java`, `dto/response/TokenResponseDTO.java`, `event/MemberRegisteredEvent.java`(신규)

**진행 기록**:
- 근거: `MemberService.java` L50-71(login), L73-82(register), L102-110(logout), L166-170(ensureEmailNotTaken). `PROGRESS.md` "회원 서비스·컨트롤러(2026-06-24)", "JWT 기반 Spring Security 인증(2026-06-24)" 기록과 일치.
- 판정(2026-07-13): 🟢 → 🟡로 변경(완료 무효화). 사유: 사용자가 `MEMBER.md`를 직접 수정하여 "회원가입 -> 회원가입 이벤트 발행" 규칙 및 "회원 가입 이벤트"(회원 id 필드)가 신규 추가됨(diff 확인). 현재 `MemberService.register()`는 어떠한 이벤트도 발행하지 않고 `MemberRegisteredEvent` 클래스도 존재하지 않아 신규 완료 기준 미충족. 재검증 방법: 재구현(이벤트 클래스 추가 + `register()` 발행 로직 추가). 사용자 승인(2026-07-13)으로 🟡 처리 및 재구현 진행 확정.
- 재구현 완료(2026-07-13):
  - 테스트 작성(4-1): `MemberServiceTest.java`에 `givenNonDuplicateEmail_whenRegister_thenSaveAndPublishMemberRegisteredEventAreInvoked`, `givenExistingEmail_whenRegister_thenThrowsMemberExceptionAndSaveAndPublishEventAreNotInvoked` 2건 추가.
  - 구현(4-2): `event/MemberRegisteredEvent.java`(신규, `BaseDomainEvent` 상속 + `memberId` 필드) 추가, `MemberService.register()`에서 저장된 회원 id로 이벤트 발행하도록 수정.
  - 리뷰(4-3): 승인(PASS). minor 지적 1건(테스트 코드에서 import된 클래스를 풀네임으로 재사용, `.claude/CLAUDE.md` 클래스 임포트 규칙) — blocker/major 아니므로 재작업 루프 대상 아님, 개선 여지로만 기록.
  - 테스트 실행(4-4): `./gradlew check` 테스트 277/277 통과, PMD 위반 없음. 단, 프로젝트 전체 라인 커버리지 79%(목표 80%)로 test-code-runner-agent 기준상 FAIL 판정. JaCoCo XML 분석 결과 `MemberRegisteredEvent`는 4/4줄(100%) 커버되며, 부족분은 `security/jwt`(19.8%), `security/refresh`(21.2%), `security/principal`(0%), `security/exception`(0%), `security/service`(25%), `subscription/infrastructure`(23.3%) 등 이번 작업과 무관한 기존 미테스트 코드에서 발생함을 확인. 사용자 확인(2026-07-13): MEMBER-02는 완료로 처리하고, 전역 커버리지 공백은 별도 크로스커팅 이슈로 `TASK.md`에 기록(아래 참고).
- 판정: 🟢 (재구현 완료 — 코드 구현 + 리뷰 승인 + 테스트 통과 277/277 + 사용자 확인, 2026-07-13)

---

## [MEMBER-03] 어드민 전용 강사 생성/정지 API

**설명**: 강사 유형 회원 생성 및 강사 정지는 어드민만 수행. 강사 정지 시 `InstructorSuspendedEvent` 발행.

**완료 기준**:
- 강사 생성/정지 엔드포인트가 어드민 전용 컨트롤러에서만 노출됨
- 강사 정지 시 `suspendedAt` 기록 + `InstructorSuspendedEvent`(강사 id 포함) 발행

**관련 규칙 위치**: `.claude/domain/MEMBER.md` "서비스 비즈니스 규칙 > 어드민", "이벤트 규칙 > 강사 정지 이벤트"

**대상 파일**: `AdminMemberController.java`, `dto/request/RegisterInstructorRequest.java`, `MemberService.java`(registerInstructor/suspendInstructor), `event/InstructorSuspendedEvent.java`

**진행 기록**:
- 근거: `MemberService.java` L84-100(registerInstructor), L130-137(suspendInstructor, 이벤트 발행). `PROGRESS.md` "어드민 전용 강사 생성/정지 API(2026-07-08)" 기록과 일치.
- 판정: 🟢 (코드 확인, 2026-07-13)

---

## [MEMBER-04] 회원 자기 관리 (비밀번호 변경 / 강사 프로필 수정 / 자진 탈퇴)

**설명**: 회원 본인의 비밀번호 변경, 강사 본인의 프로필(이름/자기소개/사진) 수정, 일반 회원 본인의 자진 탈퇴(`MemberWithdrawnEvent` 발행). 강사 자진 탈퇴는 범위 제외(2026-07-13 사용자 확정, `TASK.md` 참고).

**완료 기준**:
- 현재 비밀번호 일치 확인 후에만 비밀번호 변경
- 강사 본인만 프로필 수정 가능
- 일반 회원 자진 탈퇴 시 `withdrawnAt` 기록 + `MemberWithdrawnEvent` 발행

**관련 규칙 위치**: `.claude/domain/MEMBER.md` "도메인 모델 규칙 > 일반 회원/강사", "서비스 비즈니스 규칙"

**대상 파일**: `MemberSelfController.java`, `dto/request/ChangePasswordRequest.java`, `dto/request/UpdateInstructorProfileRequest.java`, `MemberService.java`(changePassword/updateInstructorProfile/withdrawMember)

**진행 기록**:
- 근거: `MemberService.java` L139-149(changePassword), L151-164(updateInstructorProfile), L121-128(withdrawMember, 이벤트 발행). `PROGRESS.md` "회원 자기수정 API 노출(2026-07-08)" 기록과 일치.
- 판정: 🟢 (코드 확인, 2026-07-13)
- 비고: 이메일 자기 수정(`Member.updateEmail()`)은 도메인 메서드는 존재하나 호출부 없음 — 사용자 결정으로 미구현 상태 유지(`PROGRESS.md` 기록).

---

## [MEMBER-05] 회원 정지 처리 + 이벤트 발행

**설명**: 일반 회원 정지 시 `suspendedAt` 기록 및 `MemberSuspendedEvent`(회원 id 포함) 발행. 이벤트 소비(구독권 정지 처리)는 Subscription BC 책임이며 이번 작업 범위 밖.

**완료 기준**: 정지 처리 시 `suspendedAt` not-null로 변경, `MemberSuspendedEvent` 발행

**관련 규칙 위치**: `.claude/domain/MEMBER.md` "서비스 비즈니스 규칙 > 일반 회원", "이벤트 규칙 > 회원 정지 이벤트"

**대상 파일**: `MemberService.java`(suspendMember), `event/MemberSuspendedEvent.java`

**진행 기록**:
- 근거: `MemberService.java` L112-119. `PROGRESS.md` "정지/탈퇴 상태 모델 분리(2026-07-07)", "도메인 이벤트 인프라(2026-07-08)" 기록과 일치.
- 판정: 🟢 (코드 확인, 2026-07-13)
- 의존 BC 참고: Subscription BC의 리스너 구현은 `TASK.md`의 "Member 관련 🟠 대기 항목"에 별도 기재됨(임의 계획 생성 금지 원칙에 따라 여기서 작업으로 만들지 않음).

---

## [MEMBER-06] 탈퇴 회원 이메일 마스킹

**설명**: 탈퇴 회원 조회 응답 시 이메일 앞 3글자만 노출.

**완료 기준**: `UserResponseDTO.from()` 변환 시 탈퇴 회원의 이메일이 마스킹되어 반환됨

**관련 규칙 위치**: `.claude/domain/MEMBER.md` "서비스 비즈니스 규칙 > 공통" (탈퇴된 회원은 이메일 앞 3글자만 보여준다)

**대상 파일**: `dto/response/UserResponseDTO.java`

**진행 기록**:
- 근거: `UserResponseDTO.from(Member)`에 마스킹 로직 포함 확인(이전 세션 코드 탐색 결과), `UserResponseDTOTest`(4케이스) 존재. `PROGRESS.md` "탈퇴 회원 이메일 마스킹 처리(2026-07-08)" 기록과 일치.
- 판정: 🟢 (코드 확인, 2026-07-13)

---

## 범위 제외 항목 (참고용, 작업 아님)

- 회원/강사/강좌/미션답안/미션답안댓글 신고 기능: `.claude/DO_NOT_IMPLEMENT.md`에 의해 명시적으로 구현 대상 아님.
- 강사 자진 탈퇴, 정지/탈퇴 자료 2년 보존: 2026-07-13 사용자 확정으로 이번 범위 제외(`TASK.md` 참고).
