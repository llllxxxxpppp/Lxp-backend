# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

- 구독형 LXP 백엔드 프로젝트이다.

## 프로젝트 목표

- 구독형 LXP 백엔드를 제작하는 것이 목표이다.

## 사용 기술

- 프레임워크: Spring Boot 3.5
- 언어: Java 17
- DB: H2
- ORM: Spring Data JPA
- Auth: Spring Security + JJWT가 핵심 의존성이다.

## 주요 명령어

```bash
# 빌드
./gradlew build

# 실행
./gradlew run

# 테스트 전체 실행 (JaCoCo 커버리지 보고서 자동 생성)
./gradlew test

# 단일 테스트 실행
./gradlew test --tests "com.example.SomeTest"

# PMD 정적 분석
./gradlew pmdMain
./gradlew pmdTest

# JaCoCo 보고서만 생성 (테스트 후)
./gradlew jacocoTestReport
```

## 코딩 표준 및 구체적 제약

- 클래스 이름을 패키지포함 풀네임으로 적지 말고 import 후 사용할 것.
  - ex) com.example.SomeClass -> import com.example.SomeClass;, SomeClass 사용.

## 코드 품질 도구

### PMD (정적 분석)

- 규칙 파일: `config/pmd/rules.xml`
- 활성 카테고리: `bestpractices`, `errorprone`, `multithreading`, `performance`, `security`
- 비활성 카테고리: `codestyle`, `design`, `documentation`
- 주요 커스터마이징:
    - `ignoreMagicNumbers`: 매직넘버로 `-1, 0, 1`은 사용 허용
    - `AssignmentInOperand`: 증감 연산자(`++`/`--`) 허용
- PMD 경고가 있을 경우 빌드에 실패한다. (`isIgnoreFailures = false`)

### JaCoCo (코드 커버리지)

- 테스트 실행 후 자동으로 리포트 생성 (`finalizedBy(jacocoTestReport)`)
- HTML 리포트: `build/reports/jacoco/test/html/index.html`
- XML 리포트: `build/reports/jacoco/test/jacocoTestReport.xml`

## 주요 문서

- `.claude/ARCHITECTURE.md`: 프로젝트 및 도메인 패키지 구조를 설명한 문서이다.
- `.claude/DOMAIN.md`: 필요 도메인별로 비즈니스 규칙 (불변식) 과 예상 클래스별로 책임을 명시한 문서들의 요약 문서이다.
- `.claude/PROGRESS.md`: 작업 내역 요약 문서이다.
- `.claude/TASK.md`: 작업 지시 및 현황 문서이다.
- `.claude/LOOP.md`: 마스터 에이전트의 작업 절차 관련 문서이다.

### `TASK.md` 문서 설명

`TASK.md`는 작업의 인덱스(요약표)만 유지하고, 세부 내용은 도메인별 파일로 분리한다.

- 작업 상태: ⚪ 작업대기 / 🔵 작업중 / 🟢 작업완료 / 🟡 작업보류 / 🟠 연관작업대기 / 🔴 오류
  - ⚪ 작업대기: 착수 가능한 상태
  - 🔵 작업중: 담당자(사람 또는 에이전트 세션)가 선점하여 진행 중
  - 🟢 작업완료: 완료 기준 충족 + 근거 기록 완료
  - 🟡 작업보류: 사용자 판단으로 일시 중단 (사유를 세부 파일에 기록)
  - 🟠 연관작업대기: 의존하는 선행 작업이 미완료
  - 🔴 오류: 재작업 한도 초과 등으로 사람의 개입이 필요한 상태
  - ⚫ 취소: 설계 변경 등으로 폐기된 작업. 행은 유지하고 사유를 세부 파일에 기록
- 상태 변경 권한: 🟢/🔴는 마스터 에이전트가 실행 결과에 근거해서만 변경한다. 🟡은 사용자 지시로만 변경한다.

#### 작업 요약표

- 형식: | ID | 상태 | 작업명 | 도메인 | 담당 | 의존성 |
- 한 작업 = 표의 한 행으로 유지한다. (여러 줄로 늘리지 않는다 — 병합 충돌 최소화)
- 요약표 하위에는 도메인 단위의 2~3줄 개요만 기술하고, 작업별 상세는 세부 파일에만 기록한다.

#### 세부 작업 파일

- 경로: `.claude/task/task-[도메인명].md` (예: task-order.md, task-auth.md)
- 파일 내 각 작업은 `## [작업ID] 작업명` 헤더로 구분한다.
- 각 작업에 기록할 항목: 설명, 완료 기준, 관련 규칙 위치, 대상 파일,
  진행 기록(리뷰 결과·테스트 결과·완료 근거), 보류/오류 사유(해당 시)
- 상태의 원본(source of truth)은 TASK.md 요약표이다.
  세부 파일에는 상태를 중복 기록하지 않는다. 진행 기록(사실)만 남긴다.

#### 갱신 규칙

- 작업 착수 시: 요약표의 상태를 🔵로 바꾸고 담당을 기록한 뒤 시작한다.
- 작업 완료 시: ① 세부 파일에 완료 근거 기록 → ② 요약표 상태를 🟢로 변경. 이 두 단계는 반드시 같은 커밋에 포함한다.
- 선행 작업이 🟢로 바뀌면, 그 작업에 의존하던 🟠 작업들을 ⚪로 갱신한다.

### `PROGRESS.md` 문서 설명

- 현재까지 작업된 내용들을 요약해 정리한 문서이다.
- BD 최초 계획 시 대조용으로만 읽는 읽기 전용 레거시 문서. 더 이상 갱신하지 않으며, 상태 판단의 근거는 항상 TASK.md
- `.claude/PROGRESS.md` 에 위치한다.

## 작업 절차
- `LOOP.md` 문서를 참조해서 작업을 진행한다.
