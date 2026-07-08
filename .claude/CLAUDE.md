# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Spring Boot 3.5 기반의 Java 17 웹 애플리케이션. H2 인메모리 데이터베이스를 사용하며, Spring Data JPA, Spring Security, Spring Web, JJWT가 핵심 의존성이다.

- **그룹**: `com.lcs` / **아티팩트**: `lxp`
- **데이터베이스**: H2 인메모리 (`jdbc:h2:mem:lxp-test`), DDL은 `create-drop` 전략
- **H2 콘솔**: `/h2-console` (개발 중 DB 확인용)

## 프로젝트 목표

- 구독형 LXP 백엔드를 제작하는 것이 목표이다.
- 외부 결제 시스템에 대해서는 실제 연결하지 않고 stub 객체를 만들어 시뮬레이션 한다.

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

### `PROGRESS.md` 문서 설명

- 현재까지 작업된 내용들을 요약해 정리한 문서이다.
- 새로운 에이전트가 작업을 시작하기 전 구현 완료된 부분을 빠르게 파악하고 맥락을 잡기 위해 읽는다.
- `.claude/PROGRESS.md` 에 위치한다.

## 작업 절차

1. 주요 문서들을 반드시 읽는다. (반드시 따라야 한다.)
2. 사용자의 지시 사항을 분석한다.
3. 실행 단계를 분할하고 계획한다.
4. 분할한 각각의 실행 단계를 순서대로 5번의 절차를 따라서 모두 수행한다.
5-1. 테스트 코드를 먼저 작성한다.
5-2. 코드를 구현한다.
5-3. 구현된 코드를 리뷰한다. 리뷰가 실패하면 5-2부터 다시 시작한다.
5-4. 구현된 코드를 작성된 테스트 코드를 이용하여 검증한다. 검증이 실패하면 5-1부터 다시 시작한다.
5-5. 구현된 코드를 사용자에게 정확히 구현이 되었는지 확인을 받는다.
5-6. 모든 변경 사항을 `PROGRESS.md` 파일에 기록한다.
5-7. 커밋을 생성한다. 단, 커밋 생성은 반드시 사용자에게 커밋 메시지 확인을 받고 실행할 것.
