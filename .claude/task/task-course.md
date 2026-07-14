# task-course.md

강좌 컨텐츠 관리(Course) 바운디드 컨텍스트 세부 작업 파일이다. 상태의 원본은 `.claude/TASK.md` 요약표이며, 본 파일에는 상태를 중복 기록하지 않는다.

도출 근거: `.claude/domain/COURSE.md` + `.claude/ARCHITECTURE.md` + `.claude/rules/implementation-rules.md`(인터셉터 규칙).
대조 근거: `.claude/PROGRESS.md` 강좌 컨텐츠 관리 바운디드 컨텍스트 섹션 + 실제 코드 확인(2026-07-13, 이번 세션, `src/main/java/com/lcs/lxp/course/**`).

## 계획 수립 시 확정된 사항 (2026-07-13, 사용자 확정)

- Status VO는 열거형(enum) 유지, bool(Visibility) 전환 안 함 → `COURSE.md` 반영 완료.
- 설명(description) 필드 길이 제한은 도메인 문서를 코드 기준(4096자)에 맞춰 정정 → `COURSE.md` 반영 완료.
- 강의/미션의 초기 생성 상태는 도메인 문서 기준(PUBLIC)에 코드를 맞춘다 → COURSE-01에서 코드 수정 필요(아래 참고).
- 강좌 신고 기능(COURSE.md "구독자는 강좌를 신고할 수 있다")은 `.claude/DO_NOT_IMPLEMENT.md`에 의해 범위 제외. 작업으로 만들지 않음.

---

## [COURSE-01] 강좌/강의/미션 도메인 모델 (VO·엔티티·초기 상태)

**설명**: `Title`/`ContentStatus`(Status) VO, `InstructorId`/`CourseId`/`LectureId`/`MissionId` VO, `Course`/`Lecture`/`Mission` 엔티티의 생성·유효성 검증·타임스탬프 관리. 기존 구현이 대부분을 충족하나, 강의/미션의 초기 생성 상태가 현재 PRIVATE로 되어 있어 도메인 문서 기준(PUBLIC)으로 수정 필요.

**완료 기준**:
- `Title`: blank 및 100자 초과 시 예외
- `Course`: 생성 시 상태 PRIVATE, 설명 blank/4096자 초과 시 예외, `createdAt` not-null/`updatedAt` null(생성 시), 강의·미션 각 1개 이상 있어야 `publish()` 가능
- `Lecture`/`Mission`: **생성 시 상태 PUBLIC** (현재 코드는 PRIVATE로 되어 있어 수정 대상), `createdAt` not-null/`updatedAt` null(생성 시)
- 모든 ID VO는 record 기반

**관련 규칙 위치**: `.claude/domain/COURSE.md` "공통 VO", "모든 애그리거트 공통 규칙", "강좌/강의/미션 애그리거트 도메인 모델 규칙"

**대상 파일**: `model/vo/Title.java`, `model/vo/ContentStatus.java`, `model/entity/Course.java`, `model/entity/Lecture.java`, `model/entity/Mission.java`

**진행 기록**:
- 기존 구현 확인: `Title`/`ContentStatus`/ID VO 4종, `Course`/`Lecture`/`Mission` 엔티티, 설명 4096자 검증, 강좌 초기 PRIVATE, 발행 조건(강의·미션 각 1개 이상) 모두 코드 존재 확인(`PROGRESS.md` "강좌·강의·미션 도메인 모델(2026-06-22)" 기록과 일치). 테스트: `TitleTest`(6), `CourseTest`(12), `LectureTest`(8), `MissionTest`(8) 확인됨.
- 미충족 항목 발견(2026-07-13): `Lecture.create()`/`Mission.create()`가 상태를 PRIVATE로 설정 — `COURSE.md`의 "강의는 공개 상태로 생성된다"/"미션은 공개 상태로 생성된다"와 불일치. 사용자 확정(2026-07-13)으로 코드를 PUBLIC 생성으로 수정하기로 함.
- 4-1(테스트 작성): `LectureTest`/`MissionTest`의 초기 상태 기대값을 PUBLIC으로 변경, "given private" 전제 케이스에 `unpublish()` 보강, `CourseTest`/`LectureTest`/`MissionTest`에 createdAt/updatedAt 검증 신규 추가, `CourseServiceTest`의 private 전제 2곳에 `unpublish` 호출 보강.
- 4-2(구현): `Lecture.java`(약 64행), `Mission.java`(약 70행)의 초기 상태 설정을 `ContentStatus.PRIVATE` → `ContentStatus.PUBLIC`으로 변경. 다른 파일/로직은 변경 없음.
- 4-3(리뷰): PASS. 변경 범위 정확성, 테스트-구현 정합성, COURSE-02~04 회귀 없음 확인. 참고용 minor 관찰(`CourseServiceTest` 기존 변수명 `privateLecture`/`privateMission`이 실제로는 PUBLIC 생성 후 unpublish로 상태를 맞춘 것이라 이름과 다소 어긋남 — 로직상 문제 없어 재작업 불필요) 1건.
- 4-4(테스트 실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과. 전체 커버리지 91.28%. 강의/미션 초기 상태 변경으로 인한 다른 테스트 회귀 없음 확인.
- 완료 근거: 리뷰 PASS + 테스트/PMD 통과 + 사용자 확인(2026-07-14).

---

## [COURSE-02] 강좌 생성/수정/조회 API

**설명**: 강좌 생성·제목 및 설명 수정·단건 조회(요약/상세) REST API.

**완료 기준**:
- 강좌 생성 시 강사 ID는 인증 정보에서 추출
- 공개 상태에서는 수정 요청 거부
- 상세 조회는 강의·미션 목록 포함, 요약 조회는 기본 정보만 반환

**관련 규칙 위치**: `.claude/domain/COURSE.md` "강좌 서비스 비즈니스 규칙", "모든 애그리거트 공통 규칙"

**대상 파일**: `controller/CourseController.java`, `service/CourseService.java`, `dto/request/CreateCourseRequest.java`, `dto/request/UpdateCourseRequest.java`, `dto/response/CourseSummaryResponse.java`, `dto/response/CourseDetailResponse.java`

**진행 기록**:
- 근거: `PROGRESS.md` "강좌 컨트롤러(2026-06-23)", "강좌 서비스(2026-06-22)", "강좌 컨트롤러 요청 검증(2026-06-23)", "강좌 권한 검증 로직을 Spring Security로 이관(2026-07-02)" 기록과 코드 일치 확인. 테스트: `CourseControllerTest`, `CourseServiceTest` 확인됨.
- 판정: 🟢 (코드+테스트 존재 확인, 2026-07-13)
- 비고: COURSE-01의 강의/미션 초기 상태 수정이 본 작업의 컨트롤러/서비스 로직 자체에는 영향 없음(초기 상태 값만 변경되는 것이므로). 단, COURSE-01 작업 시 회귀 여부 재확인 권장.

---

## [COURSE-03] 강좌 목록 페이지네이션·검색

**설명**: 공개 강좌 목록을 페이지네이션 및 제목 키워드 검색으로 조회.

**완료 기준**: 키워드 없으면 전체 PUBLIC 강좌, 있으면 제목 부분 일치(대소문자 무시) 검색, 페이지 응답에 `page`/`size`/`totalElements`/`totalPages`/`last` 포함

**관련 규칙 위치**: `.claude/domain/COURSE.md` "강좌 서비스 비즈니스 규칙"

**대상 파일**: `controller/CourseController.java`, `service/CourseService.java`, `repository/CourseRepository.java`, `dto/response/CoursePageResponse.java`

**진행 기록**:
- 근거: `PROGRESS.md` "강좌 목록 조회(2026-06-24)" 기록과 코드 일치 확인(`CourseRepository.findAllByStatus`/`findByStatusAndTitleKeyword`).
- 판정: 🟢 (코드+테스트 존재 확인, 2026-07-13)

---

## [COURSE-04] 강의/미션 추가 및 공개·비공개 API

**설명**: 강좌에 강의/미션 추가, 강의/미션 개별 공개·비공개 처리 API.

**완료 기준**: 강좌가 공개 상태면 추가 거부, 공개/비공개는 강사(또는 어드민) 권한으로만 수행

**관련 규칙 위치**: `.claude/domain/COURSE.md` "모든 애그리거트 공통 규칙"

**대상 파일**: `controller/CourseController.java`, `service/CourseService.java`, `dto/request/AddLectureRequest.java`, `dto/request/AddMissionRequest.java`

**진행 기록**:
- 근거: `PROGRESS.md` "강좌 컨트롤러(2026-06-23)" 엔드포인트 목록 및 `CourseService`(addLecture/publishLecture/unpublishLecture/addMission/publishMission/unpublishMission) 코드 확인.
- 판정: 🟢 (코드+테스트 존재 확인, 2026-07-13)

---

## [COURSE-05] 강의 자료 타입(확장자) 필드 도입

**설명**: 강의의 컨텐츠 자료 타입(확장자) 필드 신규 추가. 현재 `Lecture`에는 `contentUrl`만 있고 자료 타입 필드가 없음.

**완료 기준**: 강의 생성/수정 시 자료 타입(String, 확장자) NOT NULL 검증, 응답 DTO에 포함

**관련 규칙 위치**: `.claude/domain/COURSE.md` "강의 애그리거트 도메인 모델 규칙" (자료 타입 필드)

**대상 파일**: `model/entity/Lecture.java`, `model/entity/Course.java`(addLecture/updateLecture 시그니처 변경), `dto/request/AddLectureRequest.java`, `dto/response/LectureResponse.java`, `service/CourseService.java`, `controller/CourseController.java`, `security/config/SecurityConfigTest.java`(시그니처 변경에 따른 호출부 보정, 신규 로직 없음), `src/main/resources/demo-data.sql`(NOT NULL 제약에 따른 시드 데이터 보정, 신규 로직 없음)

**진행 기록**:
- 4-1(테스트 작성) 중 범위 밖 발견(T2 성격, 대상 파일 목록 보정): `SecurityConfigTest.java`가 `AddLectureRequest`/`CourseService.addLecture`의 구 시그니처(자료 타입 없음)를 호출하고 있어, 시그니처 변경 시 컴파일이 깨짐을 확인. 새 비즈니스 로직이 아닌 기계적 호출부 보정이므로 대상 파일에 추가해 같은 작업(COURSE-05) 범위에서 함께 처리하기로 함(2026-07-14).
- 채택 필드명: `contentType`(String) — 기존 `contentUrl`과의 네이밍 일관성 근거.
- 4-1(테스트 작성): `LectureTest.java` 전체 재작성(생성/수정 시 null/blank 검증 + 정상 설정 검증), `CourseTest.java`/`MissionTest.java`는 시그니처 변경에 따른 호출부만 보정, `CourseServiceTest.java`/`CourseControllerTest.java`에 서비스·컨트롤러 레벨 검증(null/blank 400, 정상 처리 시 응답 DTO 포함) 신규 추가.
- 4-2(구현) 중 추가 범위 밖 발견(대상 파일 재보정): `src/main/resources/demo-data.sql`의 `lectures` 시드 INSERT가 신규 `content_type NOT NULL` 컬럼 제약을 위반해 `@SpringBootTest`(`SecurityConfigTest` 등) 컨텍스트 로딩이 깨짐을 확인. 새 비즈니스 로직이 아닌 스키마 변경의 직접적 부수효과이므로 시드 값(`'mp4'`)만 최소 보정(2026-07-14).
- 4-2(구현): `Lecture.java`(contentType 필드+검증+getter), `Course.java`(addLecture/updateLecture 인자 추가), `AddLectureRequest.java`(`@NotBlank contentType`), `LectureResponse.java`(contentType 필드+from 매핑), `CourseService.java`(addLecture 인자 추가), `CourseController.java`(호출부 보정), `SecurityConfigTest.java`(호출부 보정), `demo-data.sql`(시드 보정) 반영.
- 4-3(리뷰): PASS. 변경 범위 정확(보고된 파일과 일치), 완료 기준 2항목 실질 충족, implementation-rules.md 준수 확인(원시값 인자/DTO 리턴, from 변환 책임, 타임스탬프 규칙 유지). `Course.updateLecture`에 대응하는 서비스/컨트롤러 엔드포인트가 COURSE-05 이전부터 없었던 기존 설계임을 확인(이번 작업 범위 누락 아님). 범위 밖 발견 2건(SecurityConfigTest 호출부, demo-data.sql 시드)이 기계적 보정에 국한됨을 확인.
- 4-4(테스트 실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과. 커버리지 91%. `@SpringBootTest` 정상 로딩 확인, 회귀 없음.
- 완료 근거: 리뷰 PASS + 테스트/PMD 통과 + 사용자 확인(2026-07-14).

---

## [COURSE-06a] 강좌/강의/미션 soft delete 모델

**설명**: `Course`/`Lecture`/`Mission` 공통으로 soft delete를 도입. 삭제된 엔티티는 어떠한 필드도 수정 불가하도록 도메인 메서드에 가드 추가. (2026-07-14 설계 변경: 별도의 boolean 플래그 없이 `deletedAt`(삭제 일시) 하나만으로 삭제 여부를 판단 — 아래 진행 기록 참고)

**완료 기준**: 각 엔티티에 `delete()` 도메인 메서드로 삭제 일시(`deletedAt`) 설정, 삭제된 엔티티(`deletedAt != null`)의 수정/공개/비공개 메서드 호출 시 예외 발생

**관련 규칙 위치**: `.claude/domain/COURSE.md` "모든 애그리거트 공통 규칙" (삭제 일시, 삭제된 데이터 수정 불가)

**대상 파일**: `model/entity/Course.java`, `model/entity/Lecture.java`, `model/entity/Mission.java`, `src/main/resources/demo-data.sql`(시드 보정, 신규 로직 없음)

**진행 기록**:
- 착수 전 확인: 현재 코드에는 물리 삭제 엔드포인트가 있었으나 제거된 상태(`PROGRESS.md` "강의·미션 물리 삭제 엔드포인트 제거(2026-06-23)")이며, soft delete는 아직 어떤 형태로도 구현되어 있지 않음.
- 4-1(테스트 작성): `CourseTest`/`LectureTest`/`MissionTest`에 `delete()` 호출 시 삭제 상태 반영, 삭제된 엔티티의 수정/공개/비공개/재삭제 시 예외 발생 케이스 추가. `Course`의 하위 강의/미션 위임 메서드 9개에 대한 "삭제된 강좌를 통한 조작 차단" 케이스 포함.
- 4-2(구현, 1차): `deleted`(boolean)+`deletedAt`(OffsetDateTime) 두 필드 도입, `isDeleted()`/`getDeletedAt()` 게터, `delete()`, `checkNotDeleted()` 가드를 각 엔티티의 모든 수정성 메서드에 삽입.
- 4-3(리뷰, 1차): PASS. 가드 커버리지, 재삭제 방지, 타임스탬프 규칙, COURSE-01~05 회귀 없음 확인.
- 4-4(테스트 실행, 1차): FAIL — `demo-data.sql` 시드가 신규 `deleted`(NOT NULL) 컬럼을 채우지 않아 `@SpringBootTest` 컨텍스트 초기화 실패(40개 테스트 FAIL). 구현/스키마 문제로 판별.
- 4-2(재작업 1회차): `demo-data.sql`의 `courses`/`lectures`/`missions` INSERT에 `deleted`(FALSE)/`deleted_at`(NULL) 값 보정.
- 4-3(재리뷰): PASS. 컬럼/값 개수 일치, 순수 시드 보정 확인.
- 4-4(재실행): PASS. BUILD SUCCESSFUL, PMD 통과, 커버리지 91%, 294개 테스트 전체 통과.
- **설계 변경(2026-07-14, 사용자 지시)**: 완료 처리/커밋 전 사용자가 `deleted`(boolean)와 `deletedAt`이 중복 정보라는 점을 지적 — `delete()`라는 단일 진입점에서만 둘이 동시에 세팅되는 구조이므로 `deletedAt != null` 하나로 삭제 여부를 판단해도 상태 불일치 위험이 없음을 확인. `.claude/domain/COURSE.md` "모든 애그리거트 공통 규칙" 가변 데이터 필드 항목에서 "soft delete 플래그: bool 타입" 삭제, "삭제 일시가 NULL이 아니면 삭제된 것으로 판단한다"로 명시(사용자 승인). 이 작업은 아직 완료 처리·커밋 전이었으므로 완료 무효화(🟡) 절차 없이 같은 사이클 내에서 재구현으로 처리.
- 4-2(재구현): `deleted` 필드 완전 제거, `isDeleted()` → `return deletedAt != null;`, `checkNotDeleted()` → `if (deletedAt != null)`, `delete()`는 `deletedAt`만 세팅, `create()`의 `deleted=false` 초기화 제거. `demo-data.sql`의 `deleted` 컬럼/값 제거(`deleted_at`만 유지). 테스트는 `isDeleted()` 공개 API만 참조하므로 무수정.
- 4-3(재구현 리뷰): PASS. minor 2건(테스트명/`@DisplayName`에 "삭제 플래그" 문구 잔존, 진행 기록 서술 정확성) — 기능상 문제 없어 재작업 불필요, 참고 기록만 함.
- 4-4(재구현 테스트 실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과. 커버리지 92.15%. 회귀 없음.
- 완료 근거: 리뷰 PASS + 테스트/PMD 통과 + 사용자 확인(2026-07-14).

---

## [COURSE-06b] 강좌/강의/미션 삭제 API

**설명**: 강사·어드민 권한으로 강좌/강의/미션을 soft delete 처리하는 REST API.

**완료 기준**: 강사 본인 또는 어드민만 삭제 가능, 삭제 성공 시 200/204 응답

**관련 규칙 위치**: `.claude/domain/COURSE.md` "모든 애그리거트 공통 규칙" (삭제는 강사와 어드민만)

**대상 파일**: `controller/CourseController.java`, `service/CourseService.java`, `security/config/SecurityConfig.java`

**의존성**: COURSE-06a

**진행 기록**: (미착수)

---

## [COURSE-07a] Sortable 인터페이스 + 순번 필드

**설명**: 강의·미션이 구현하는 `Sortable` 인터페이스 정의, 강좌 단위 고유 순번 필드 도입. 강좌에 강의/미션 추가 시 자동으로 다음 순번 부여.

**완료 기준**: `Sortable` 인터페이스에 순번 getter 정의, `Lecture`/`Mission` 구현, 같은 강좌 내 순번 중복 없음, 추가 시 자동 할당

**관련 규칙 위치**: `.claude/domain/COURSE.md` "모든 애그리거트 공통 규칙"(인터페이스: Sortable), "강좌 애그리거트 도메인 모델 규칙"(Sortable 리스트·고유 순번)

**대상 파일**: `model/vo/Sortable.java`(신규), `model/entity/Lecture.java`, `model/entity/Mission.java`, `model/entity/Course.java`

**진행 기록**: (미착수) — 현재 코드에는 순번 개념이 전혀 없음(리스트 추가 순서만 존재).

---

## [COURSE-07b] 강좌 단위 순서 변경 API

**설명**: 강좌 내 강의·미션의 노출 순서를 변경하는 API.

**완료 기준**: 순서 변경 요청 시 같은 강좌 소속 여부·중복 없는 순번 재배치 검증, 강사(또는 어드민) 권한만 허용

**관련 규칙 위치**: `.claude/domain/COURSE.md` "강좌 애그리거트 도메인 모델 규칙" (Sortable 객체 순서 변경)

**대상 파일**: `controller/CourseController.java`, `service/CourseService.java`, `dto/request/ReorderRequest.java`(신규)

**의존성**: COURSE-07a

**진행 기록**: (미착수)

---

## [COURSE-08a] InstructorSuspendedEvent 리스너 (정지 강사 강좌 비공개 처리)

**설명**: 강사 정지 이벤트(`InstructorSuspendedEvent`, Member BC 발행)를 구독해 해당 강사의 모든 강좌를 비공개 처리.

**완료 기준**: 이벤트 수신 시 해당 강사 소유의 모든 PUBLIC 강좌가 PRIVATE로 전환, 리스너가 처리 시작/종료를 info 레벨로 로그 기록(`ARCHITECTURE.md` 이벤트 규칙)

**관련 규칙 위치**: `.claude/PROGRESS.md` "후속 구현 필요" 항목(강사 정지 시 강좌 비공개), `.claude/ARCHITECTURE.md` "바운디드 컨텍스트별 event 패키지"

**대상 파일**: `event/InstructorSuspendedEventListener.java`(신규), `repository/CourseRepository.java`(강사 ID 기준 조회 쿼리 추가), `service/CourseService.java`(대량 비공개 처리 메서드 추가)

**의존성**: MEMBER-03(🟢, `InstructorSuspendedEvent` 발행 주체)

**진행 기록**: (미착수) — `TASK.md`의 "Member 관련 🟠 대기 항목"에 있던 자리표시자를 이번 계획에서 COURSE-08a/08b로 구체화함(치환 근거: MEMBER-03이 이미 🟢로 완료되어 의존성 충족).

---

## [COURSE-08b] 정지된 강사 2차 방어 인터셉터

**설명**: 강좌/강의/미션의 생성·수정·공개·비공개 요청에 대해, 요청자가 정지된 강사인 경우 (JWT가 유효하더라도) 요청을 차단하는 2차 방어 로직.

**완료 기준**: 정지된 강사의 강좌/강의/미션 생성·수정·공개·비공개 요청이 403으로 거부됨

**관련 규칙 위치**: `.claude/rules/implementation-rules.md` "인터셉터 규칙", `.claude/domain/MEMBER.md` "인터셉터 규칙"(2026-07-13 추가, 내용 동일)

**대상 파일**: `security/config/SecurityConfig.java` 또는 신규 필터/인터셉터 클래스, `service/CourseService.java`(정지 여부 확인 지점 추가)

**의존성**: COURSE-08a (동일 이벤트/조회 인프라 공유 가능성 있음 — 착수 시 재검토)

**진행 기록**: (미착수)

---

## 범위 제외 항목 (참고용, 작업 아님)

- 강좌 신고 기능(`COURSE.md` "구독자는 강좌를 신고할 수 있다"): `.claude/DO_NOT_IMPLEMENT.md`에 의해 명시적으로 구현 대상 아님.
