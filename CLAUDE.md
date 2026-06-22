# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Spring Boot 3.5 기반의 Java 17 웹 애플리케이션. H2 인메모리 데이터베이스를 사용하며, Spring Data JPA, Spring Security, Spring Web이 핵심 의존성이다.

- **그룹**: `com.lcs` / **아티팩트**: `lxp`
- **데이터베이스**: H2 인메모리 (`jdbc:h2:mem:lxp-test`), DDL은 `create-drop` 전략
- **H2 콘솔**: `/h2-console` (개발 중 DB 확인용)

## 주요 명령어

```bash
# 빌드
./gradlew build

# 모든 테스트 및 PMD 정적 분석 실행
./gradlew check

# 테스트 실행 (JaCoCo 커버리지 리포트도 함께 생성됨)
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.lcs.lxp.SomeTest"

# 단일 테스트 메서드 실행
./gradlew test --tests "com.lcs.lxp.SomeTest.methodName"

# PMD 정적 분석
./gradlew pmdMain

# 애플리케이션 실행
./gradlew bootRun

# JaCoCo 커버리지 리포트만 생성 (build/jacocoHtml/index.html)
./gradlew jacocoTestReport
```

## 코드 품질 도구

### PMD (정적 분석)
- 규칙 파일: `config/pmd/rules.xml`
- `isIgnoreFailures = false` — PMD 위반 시 빌드 실패
- 활성화된 카테고리: `bestpractices`, `errorprone`, `multithreading`, `performance`, `security`
- `codestyle`, `design`, `documentation`은 비활성화 상태
- 주요 커스터마이징:
  - `ignoreMagicNumbers`: `-1, 0, 1`은 허용
  - `AssignmentInOperand`: 증감 연산자(`++`/`--`)는 허용

### JaCoCo (코드 커버리지)
- 테스트 실행 후 자동으로 리포트 생성 (`finalizedBy(jacocoTestReport)`)
- HTML 리포트: `build/jacocoHtml/index.html`
- XML 리포트: `build/reports/jacoco/test/jacocoTestReport.xml`

## 프로젝트 목표

- 구독형 LXP 백엔드를 제작하는 것이 목표이다.
- 외부 결제 시스템에 대해서는 실제 연결하지 않고 dummy 객체를 만들어 시뮬레이션 한다.

## 주요 문서

- `.claude/ARCHITECTURE.md`: 프로젝트 및 도메인 패키지 구조를 설명한 문서이다.
- `.claude/DOMAIN.md`: 필요 도메인별로 비즈니스 규칙 (불변식) 과 예상 클래스별로 책임을 명시한 문서들의 요약 문서이다.
- `.claude/PROGRESS.md`: 구현이 완료된 기능들이 요약되어있는 문서이다.

## 구현 규칙

- Controller는 `@RestController`를 이용하여 구현한다.
- 모든 Service 클래스는 상위 계층과 데이터를 주고받을 때 반드시 DTO를 이용하도록 구현한다.

## 작업 절자

1. 주요 문서들을 읽는다.
2. 사용자로부터 구현이 필요한 기능을 입력받는다.
3. 명세서에 의거하여 테스트 코드를 작성한다.
4. 명세서에 의거하여 필요한 기능을 구현한다.
5. 테스트를 실행하여 충분한 리뷰 기준을 통과하는지 확인한다.
6. 완료된 기능에 대해 `.claude/PROGRESS.md` 파일에 기록한다.
