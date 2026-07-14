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

**대상 파일**: `controller/CourseController.java`, `service/CourseService.java`, `security/config/SecurityConfig.java`, `model/entity/Course.java`(deleteLecture/deleteMission 위임 메서드 추가, 기존 publishLecture 등과 동일 패턴, 신규 로직 없음)

**의존성**: COURSE-06a

**진행 기록**:
- 계획 수립 중 발견(2026-07-14): 완료 기준 문구("강사 본인 또는 어드민만")가 기존 코드(수정/공개/비공개 API 전체)의 소유권 미검증 상태와 다름을 확인. 사용자 확정(2026-07-14)으로 COURSE-06b는 기존 역할 기반 패턴만 재사용하고, 소유권 검증은 COURSE-09로 분리 등록.
- 4-1(테스트 작성): `CourseServiceTest`/`CourseControllerTest`/`SecurityConfigTest`에 강좌/강의/미션 삭제(`DELETE /api/courses/{courseId}`, `.../lectures/{lectureId}`, `.../missions/{missionId}`, 200 응답, INSTRUCTOR/ADMIN 허용·MEMBER 403·미인증 401) 테스트 추가.
- 4-2(구현): `CourseController`(3개 `@DeleteMapping`), `CourseService`(deleteCourse/deleteLecture/deleteMission), `SecurityConfig`(3개 경로 `hasAnyRole("INSTRUCTOR","ADMIN")`) 추가. `Course.java`에 `deleteLecture`/`deleteMission` 위임 메서드 추가(기존 publishLecture 등과 동일 패턴, package-private `Lecture.delete()`/`Mission.delete()` 호출을 위해 불가피).
- 4-3(리뷰): PASS. 변경 범위 정확, 완료 기준 충족(역할 기반 200/403/401), 위임 메서드가 불가피한 최소 변경임을 확인, COURSE-09 범위 침범 없음, 회귀 없음.
- 4-4(테스트 실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과. 커버리지 91%.
- 완료 근거: 리뷰 PASS + 테스트/PMD 통과 + 사용자 확인(2026-07-14).

---

## [COURSE-07a] Sortable 인터페이스 + 순번 필드

**설명**: 강의·미션이 구현하는 `Sortable` 인터페이스 정의, 강좌 단위 고유 순번 필드 도입. 강좌에 강의/미션 추가 시 자동으로 다음 순번 부여.

**완료 기준**: `Sortable` 인터페이스에 순번 getter 정의, `Lecture`/`Mission` 구현, 같은 강좌 내 순번 중복 없음, 추가 시 자동 할당

**관련 규칙 위치**: `.claude/domain/COURSE.md` "모든 애그리거트 공통 규칙"(인터페이스: Sortable), "강좌 애그리거트 도메인 모델 규칙"(Sortable 리스트·고유 순번)

**대상 파일**: `model/vo/Sortable.java`(신규), `model/entity/Lecture.java`, `model/entity/Mission.java`, `model/entity/Course.java`, `src/main/resources/demo-data.sql`(시드 보정, 신규 로직 없음)

**진행 기록**:
- 착수 전 확인: 현재 코드에는 순번 개념이 전혀 없음(리스트 추가 순서만 존재).
- 설계 근거: `Sortable.getSortOrder()`(단일 SAM, `@FunctionalInterface`), 순번은 강의+미션 통합 시퀀스(도메인 문서의 "Sortable 객체 리스트"(단수) 표현 근거), 순번 할당은 `Lecture.create`/`Mission.create` 팩토리 파라미터로 전달(`Course.addLecture`/`addMission`의 공개 시그니처는 불변).
- 4-1(테스트 작성): `CourseTest`/`LectureTest`/`MissionTest`에 Sortable 구현 여부, 순번 자동 할당(1,2,3...), 순번 중복 없음 검증 테스트 추가.
- 4-2(구현, 1차): `Sortable.java` 신규, `Lecture`/`Mission`에 `sortOrder` 필드+게터, `Course.nextSortOrder() = lectures.size()+missions.size()+1`로 계산. `demo-data.sql` 시드에 `sort_order` 값 보정.
- 4-3(리뷰, 1차): **blocker** — `nextSortOrder()`의 size 기반 계산이 `removeLecture`/`removeMission`(물리 제거) 이후 재추가 시 기존 순번과 중복될 수 있음을 재현 시나리오로 확인.
- 4-2(재작업 1회차): `nextSortOrder()`를 "강의+미션(soft delete 포함) 전체 중 최대 sortOrder + 1" 방식으로 변경.
- 4-3(재리뷰 1회차): PASS. 재현 시나리오 해소 확인. minor(회귀 테스트 부재) 1건 — 재작업 필수는 아니나 보강 권장.
- 회귀 테스트 보강: `CourseTest`에 물리 제거 후 재추가 시 순번이 최댓값+1이 되고 중복되지 않음을 검증하는 테스트 추가.
- 4-4(테스트 실행, 1차): **FAIL** — `Sortable.java`에 `@FunctionalInterface` 누락으로 PMD `ImplicitFunctionalInterface` 위반.
- 4-2(재작업 2회차): `Sortable.java`에 `@FunctionalInterface` 어노테이션 추가.
- 4-3(재리뷰 2회차): PASS. PMD 통과 확인.
- 4-4(재실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과. 커버리지 91%.
- 완료 근거: 리뷰 PASS(재작업 2회, 한도 3회 이내) + 테스트/PMD 통과 + 사용자 확인(2026-07-14).

---

## [COURSE-07b] 강좌 단위 순서 변경 API

**설명**: 강좌 내 강의·미션의 노출 순서를 변경하는 API.

**완료 기준**: 순서 변경 요청 시 같은 강좌 소속 여부·중복 없는 순번 재배치 검증, 강사(또는 어드민) 권한만 허용

**관련 규칙 위치**: `.claude/domain/COURSE.md` "강좌 애그리거트 도메인 모델 규칙" (Sortable 객체 순서 변경)

**대상 파일**: `controller/CourseController.java`, `service/CourseService.java`, `dto/request/ReorderRequest.java`(신규), `model/vo/ReorderItem.java`(신규), `model/vo/SortableType.java`(신규), `model/entity/Course.java`, `model/entity/Lecture.java`, `model/entity/Mission.java`, `security/config/SecurityConfig.java`

**의존성**: COURSE-07a

**진행 기록**:
- 4-1(테스트 작성): `CourseTest`(reorder), `CourseServiceTest`(reorderItems), `CourseControllerTest`(reorderItems), `SecurityConfigTest`(PATCH /api/courses/{courseId}/reorder 권한) 테스트 추가. 설계: `{type, id}` 순서 리스트를 받아 서버가 순차 재할당(클라이언트가 숫자 직접 지정 안 함), 부분 재배치 불허(전체 개수 일치 요구).
- 4-2(구현, 1차): `ReorderItem`/`SortableType`/`ReorderRequest` 신규, `Course.reorder(List<ReorderItem>)`(개수 불일치/중복/미소속 검증 후 활성 항목만 1..N 재할당), `Lecture`/`Mission`에 `assignSortOrder(int)` 추가, `CourseService.reorderItems`, `CourseController`의 `PATCH /{courseId}/reorder`, `SecurityConfig`에 `hasAnyRole("INSTRUCTOR","ADMIN")` 추가.
- 4-3(리뷰, 1차): **major 2건** — (1) `assignSortOrder`가 `updatedAt` 미갱신, (2) soft-delete 제외 재배치(활성 항목만 1..N)가 삭제된 항목의 기존 순번과 충돌 가능(재현 시나리오 확인).
- 4-2(재작업 1회차): `assignSortOrder`에 `updatedAt` 갱신 추가. `Course.reorder()`의 재배치 시작 기준값을 `maxSortOrderAmongDeleted()`(삭제 항목의 최댓값)로 변경해 충돌 방지. 다른 구조 변경 메서드와의 일관성을 위해 `reorder()`에 PUBLIC 상태 가드도 추가(범위 밖 관찰에 대한 일관성 적용).
- 4-3(재리뷰): PASS. 재현 시나리오(다양한 삭제 패턴) 해소 확인, PUBLIC 가드 검증.
- 테스트 보강: PUBLIC 가드에 대한 전용 회귀 테스트(`givenPublicCourse_whenReorder_thenThrowsException`) 추가.
- 4-4(테스트 실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과. 커버리지 92%.
- 완료 근거: 리뷰 PASS(재작업 1회) + 테스트/PMD 통과 + 사용자 확인(2026-07-14). (COURSE-10 작업 중 발견된 사항 — `getLectures()`/`getMissions()`가 sortOrder 정렬을 보장하지 않고 API 응답에 sortOrder가 노출되지 않는 문제는 COURSE-10에서 별도로 보강.)

---

## [COURSE-08a] InstructorSuspendedEvent 리스너 (정지 강사 강좌 비공개 처리)

**설명**: 강사 정지 이벤트(`InstructorSuspendedEvent`, Member BC 발행)를 구독해 해당 강사의 모든 강좌를 비공개 처리.

**완료 기준**: 이벤트 수신 시 해당 강사 소유의 모든 PUBLIC 강좌가 PRIVATE로 전환, 리스너가 처리 시작/종료를 info 레벨로 로그 기록(`ARCHITECTURE.md` 이벤트 규칙)

**관련 규칙 위치**: `.claude/PROGRESS.md` "후속 구현 필요" 항목(강사 정지 시 강좌 비공개), `.claude/ARCHITECTURE.md` "바운디드 컨텍스트별 event 패키지"

**대상 파일**: `event/InstructorSuspendedEventListener.java`(신규), `repository/CourseRepository.java`(강사 ID 기준 조회 쿼리 추가), `service/CourseService.java`(대량 비공개 처리 메서드 추가)

**의존성**: MEMBER-03(🟢, `InstructorSuspendedEvent` 발행 주체)

**진행 기록**:
- 착수 전 확인: `TASK.md`의 "Member 관련 🟠 대기 항목"에 있던 자리표시자를 이번 계획에서 COURSE-08a/08b로 구체화함(치환 근거: MEMBER-03이 이미 🟢로 완료되어 의존성 충족).
- 4-1(테스트 작성): `CourseRepositoryTest`(신규, `@DataJpaTest`), `CourseServiceTest`(unpublishAllByInstructor 3개 테스트), `InstructorSuspendedEventListenerTest`(신규, Mockito) 작성.
- 4-2(구현, 1차): `CourseRepository.findAllByInstructorIdAndStatus`, `CourseService.unpublishAllByInstructor`, `InstructorSuspendedEventListener`(신규, `@TransactionalEventListener(AFTER_COMMIT)`, 시작/종료 info 로그) 추가.
- 4-3(리뷰, 1차): **major** — soft delete되었으나 status가 PUBLIC으로 남은 강좌가 조회에 포함되면 `unpublish()` 호출 시 예외로 배치(해당 강사의 다른 강좌 포함) 전체가 롤백될 위험 확인. `@TransactionalEventListener(AFTER_COMMIT)` 선택 근거(`MemberService.suspendInstructor`가 `@Transactional` 내부에서 이벤트 발행)는 타당함을 확인.
- 4-2(재작업 1회차): `findAllByInstructorIdAndStatusAndDeletedAtIsNull`로 변경(`deletedAt IS NULL` 조건 추가), 호출부 보정.
- 회귀 테스트 보강: `CourseRepositoryTest`에 soft delete된 PUBLIC 강좌가 조회에서 제외됨을 검증하는 테스트 추가.
- 4-3(재리뷰): PASS. 파생 쿼리 정확성, 회귀 테스트 실효성, 기존 케이스 유지, 리스너 미변경(범위 준수) 확인.
- 4-4(테스트 실행, 1차): **FAIL** — 회귀 테스트의 PMD 위반(`LiteralsFirstInComparisons`).
- 4-1(테스트 재작업): 문자열 리터럴을 비교 앞쪽에 두도록 수정(검증 로직 변경 없음).
- 4-4(재실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과. 커버리지 92%.
- 완료 근거: 리뷰 PASS(재작업 2회, 한도 3회 이내) + 테스트/PMD 통과 + 사용자 확인(2026-07-14).

---

## [COURSE-08b] 정지된 강사 2차 방어 인터셉터

**설명**: 강좌/강의/미션의 생성·수정·공개·비공개 요청에 대해, 요청자가 정지된 강사인 경우 (JWT가 유효하더라도) 요청을 차단하는 2차 방어 로직.

**완료 기준**: 정지된 강사의 강좌/강의/미션 생성·수정·공개·비공개 요청이 403으로 거부됨

**관련 규칙 위치**: `.claude/rules/implementation-rules.md` "인터셉터 규칙", `.claude/domain/MEMBER.md` "인터셉터 규칙"(2026-07-13 추가, 내용 동일)

**대상 파일**: `security/aspect/RejectSuspendedInstructor.java`(신규), `security/aspect/SuspendedInstructorAspect.java`(신규), `security/exception/SuspendedInstructorException.java`(신규), `course/exception/CourseExceptionHandler.java`, `course/service/CourseService.java`(애노테이션 부착), `course/controller/CourseController.java`(instructorId 추출 버그 수정), `build.gradle.kts`(spring-boot-starter-aop 추가)

**의존성**: COURSE-08a (동일 이벤트/조회 인프라 공유 가능성 있음 — 착수 시 재검토 결과: 직접 공유 없음, 독립적으로 구현)

**진행 기록**:
- 1차 시도(Spring Security 필터): `SuspendedInstructorFilter`(`OncePerRequestFilter`)로 구현, 리뷰 PASS, 테스트 PASS까지 진행했으나 완료 처리 전 사용자 논의를 통해 설계 재검토.
- **설계 변경(2026-07-14, 사용자 확정)**: 필터 방식(URL 패턴 수동 관리, 엔드포인트 변경 시 동기화 위험)에서 AOP 방식으로 재설계. 필터 기반 구현(미커밋 상태였음)을 전부 되돌리고 처음부터 재작업.
- 4-1(테스트 작성): `SuspendedInstructorAspectTest`(신규, 순수 Mockito 단위 테스트), `SuspendedInstructorAspectIntegrationTest`(신규, `@SpringBootTest`+`MockMvc`, `MemberRepository`만 목킹해 실제 `CustomUserPrincipal` 인증으로 AOP 위빙+403 응답 end-to-end 검증) 작성.
- 4-2(구현, 1차): `spring-boot-starter-aop` 추가, `RejectSuspendedInstructor`(마커 애노테이션), `SuspendedInstructorAspect`(`@Before`), `SuspendedInstructorException`, `CourseExceptionHandler`에 403 매핑, `CourseService`의 생성/수정/공개/비공개 10개 메서드에 애노테이션 부착.
- 4-3(리뷰, 1차): PASS. 대상 메서드 10개 정확성, self-invocation 우회 가능성, 예외 계층, 프록시 모드 모두 확인.
- 4-4(테스트 실행, 1차): **FAIL** — 통합 테스트가 실제 `CustomUserPrincipal`(이메일 기반 username)로 검증하다가, 기존(COURSE-02) `CourseController.createCourse()`의 `Long.parseLong(authentication.getName())` 버그를 발견(실제 JWT 인증 시 이메일이 반환되어 항상 `NumberFormatException`). 동일 버그가 `SubscriptionController`(2곳), `MemberSelfController`(1곳)에도 존재함을 확인.
- **범위 확정(2026-07-14, 사용자 확정)**: COURSE-08b 범위에서 `CourseController`만 최소 수정. `MemberSelfController`(MEMBER-04)는 별도로 지금 함께 수정(완료 무효화 절차). `SubscriptionController`는 지금 수정하지 않고 `TASK.md` 크로스커팅 발견 사항에 기록만 하여 추후 SUB-0X 착수 시 반영.
- 4-2(재작업 1회차): `CourseController.createCourse()`를 `CustomUserPrincipal.getUserId()` 사용으로 수정. `CourseControllerTest`의 관련 `@WithMockUser` 우회 테스트를 실제 `CustomUserPrincipal` 주입 방식으로 보정.
- 4-3(재리뷰 1회차): PASS(minor 1건, 기록용 — SecurityConfig의 실제 권한 설정과 마스터 서술 간 사소한 표현 차이, 결론에 영향 없음).
- 4-4(테스트 실행, 2차): **FAIL** — `SecurityConfigTest`에도 동일한 `@WithMockUser` 우회 패턴이 남아있어 캐스팅 예외 발생.
- 4-1(테스트 재작업): `SecurityConfigTest`의 `createCourse` 테스트를 동일하게 실제 `CustomUserPrincipal` 주입 방식으로 보정.
- 4-4(재실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과. 커버리지 93%.
- 완료 근거: 리뷰 PASS(재작업 2회, 한도 3회 이내) + 테스트/PMD 통과 + 사용자 확인(2026-07-14).

---

## [COURSE-09] 강좌/강의/미션 소유권(작성 강사 본인) 검증 전체 적용

**설명**: 현재 강좌/강의/미션의 수정·공개·비공개·삭제 API는 전부 역할 기반(`hasAnyRole("INSTRUCTOR", "ADMIN")`) 검사만 수행하고, 요청자가 실제로 그 강좌를 작성한 강사 본인인지 확인하는 소유권 검증이 없다(강사 A가 강사 B의 강좌를 수정/공개/비공개/삭제할 수 있는 상태). COURSE-06b 계획 수립 중 발견(2026-07-14)됨. 사용자 확정(2026-07-14)으로 COURSE-06b는 기존 역할 기반 패턴을 그대로 유지하고, 소유권 검증은 이 작업에서 강좌/강의/미션의 수정·공개·비공개·삭제 API 전체에 일괄 적용한다.

**완료 기준**: 강좌/강의/미션의 수정·공개·비공개·삭제 요청 시, 요청자가 어드민이거나 해당 강좌의 작성 강사 본인(`Course.instructorId`와 인증된 사용자 ID 일치)인 경우에만 허용, 그 외(다른 강사 등)는 403 응답

**관련 규칙 위치**: `.claude/domain/COURSE.md` "모든 애그리거트 공통 규칙"("생성은 강사만", "공개는 강사만", "비공개는 강사와 어드민만", "삭제는 강사와 어드민만" — "강사"가 작성자 본인을 의미하는지 여부를 이 작업에서 확정)

**대상 파일**: `controller/CourseController.java`, `service/CourseService.java` (또는 신규 인가 로직 위치), 영향받는 기존 테스트: `CourseServiceTest.java`, `CourseControllerTest.java`

**진행 기록**: (미착수) — COURSE-02(강좌 수정), COURSE-04(강의/미션 추가·공개·비공개), COURSE-06b(삭제)는 이미 완료 처리되었으나 완료 기준 자체에 소유권 검증이 없었으므로 이번 발견으로 인해 완료 무효화 대상은 아님(범위 확장 신규 작업). 착수 시 위 3개 작업의 기존 테스트가 소유권 검증 추가로 깨지지 않는지 함께 확인 필요.

---

## [COURSE-10] 강의/미션 순서 보장 강화 및 물리 삭제 제거

**설명**: 2026-07-14 `COURSE.md`에 "삭제는 soft delete를 한다"가 명시적으로 추가되면서, `Course.java`에 남아있던 물리 삭제 메서드(`removeLecture`/`removeMission`, 어떤 API에서도 호출되지 않는 죽은 코드)가 문서와 형식적으로 어긋남을 발견. 또한 사용자가 "강좌 내 강의/미션은 동일 리스트에서 관리되고 순서가 보장되어야 한다"는 설계 의도를 확인하는 과정에서, 실제로는 (1) `getLectures()`/`getMissions()`가 `sortOrder` 기준 정렬 없이 반환되고, (2) API 응답(`LectureResponse`/`MissionResponse`/`CourseDetailResponse`)에 `sortOrder`가 전혀 노출되지 않아 클라이언트가 강의+미션의 실제 노출 순서를 알 방법이 없다는 두 가지 문제를 추가로 확인함. DB 테이블 통합(Lecture/Mission Single Table Inheritance)은 대규모 변경이라 범위에서 제외하고, 논리적 통합 뷰 + API 노출 보강으로 범위를 한정하기로 사용자 확정(2026-07-14).

**완료 기준**:
- `Course.getLectures()`/`getMissions()`가 `sortOrder` 기준 오름차순으로 정렬되어 반환된다
- `Course.getSortableItems()`(신규)가 강의+미션을 합쳐 `sortOrder` 기준으로 정렬한 통합 뷰를 반환한다
- `LectureResponse`/`MissionResponse`에 `sortOrder` 필드가 포함된다
- `CourseDetailResponse`에 강의+미션을 타입 구분과 함께 `sortOrder` 순으로 병합한 통합 목록(`items`)이 포함된다(기존 `lectures`/`missions` 개별 배열은 하위 호환을 위해 유지, 통합 배열을 추가로 제공 — 사용자 확정 옵션 (b))
- `Course.removeLecture(LectureId)`/`removeMission(MissionId)`(물리 삭제) 및 이를 호출하는 테스트가 제거된다

**관련 규칙 위치**: `.claude/domain/COURSE.md` "모든 애그리거트 공통 규칙"(삭제는 soft delete를 한다), "강좌 애그리거트 도메인 모델 규칙"(Sortable 객체 리스트, 고유 순번, 노출 순서 변경)

**대상 파일**: `model/entity/Course.java`, `dto/response/LectureResponse.java`, `dto/response/MissionResponse.java`, `dto/response/CourseDetailResponse.java`, `dto/response/CourseItemResponse.java`(신규, 통합 목록 항목 표현), `src/test/java/com/lcs/lxp/course/model/entity/CourseTest.java`, `src/test/java/com/lcs/lxp/course/controller/CourseControllerTest.java`, `src/test/java/com/lcs/lxp/course/service/CourseServiceTest.java`(필요 시)

**의존성**: COURSE-06a, COURSE-07a, COURSE-07b (모두 완료)

**영향 범위 참고**: `CourseDetailResponse`는 COURSE-02(🟢)의 대상 파일이었으나, 이번 변경은 필드 추가(응답 구조 확장)이며 COURSE-02의 완료 기준(강사 ID 추출, 공개 상태 수정 거부, 상세/요약 구분) 자체는 변경되지 않으므로 완료 무효화 대상은 아님. 관련 테스트 재실행으로 회귀 없음 확인함.

**진행 기록**:
- 4-1(테스트 작성): `CourseTest`(getLectures/getMissions 정렬, getSortableItems 병합 정렬 검증 + removeLecture/removeMission 관련 테스트 9개 삭제), `CourseServiceTest`/`CourseControllerTest`(sortOrder/items 관련 단정 추가).
- 4-2(구현): `Course.getLectures()`/`getMissions()`를 `sortOrder` 기준 정렬 반환으로 수정, `getSortableItems()` 신규(강의+미션 병합 정렬), `removeLecture`/`removeMission`(물리 삭제, 죽은 코드) 완전 제거. `LectureResponse`/`MissionResponse`에 `sortOrder` 필드 추가(끝에), `CourseItemResponse`(신규) 추가, `CourseDetailResponse`에 `items` 필드 추가(끝에, `getSortableItems()` 재사용).
- 4-3(리뷰): PASS. 변경 범위 정확, 완료 기준 5항목 실질 충족, `implementation-rules.md` 준수(Entity→DTO는 from 메서드 책임), COURSE-02/04/06b/07a/07b 회귀 없음 확인.
- 4-4(테스트 실행): `./gradlew check` BUILD SUCCESSFUL. PMD 통과. 커버리지 92%+.
- 커밋 분리: COURSE-07b와 같은 워킹트리에서 동시에 작업되어, 완료 확인 지연 중 두 작업이 뒤섞인 상태로 리뷰/테스트가 진행됨. 사용자 요청(2026-07-14)에 따라 COURSE-07b를 먼저 완료 처리·커밋(f695752)한 뒤, COURSE-10 변경분을 정확히 복원해 별도 커밋으로 분리함(재검증: `./gradlew check` 재실행 PASS 확인).
- 완료 근거: 리뷰 PASS + 테스트/PMD 통과 + 사용자 확인(2026-07-14).

---

## 범위 제외 항목 (참고용, 작업 아님)

- 강좌 신고 기능(`COURSE.md` "구독자는 강좌를 신고할 수 있다"): `.claude/DO_NOT_IMPLEMENT.md`에 의해 명시적으로 구현 대상 아님.
