# 트러블슈팅 기록

구현 중 발생한 문제와 해결 내역을 기록한다.

형식:
- **발생 시점**: 어느 단계에서 발생했는지
- **문제**: 무슨 문제가 생겼는지
- **원인**: 왜 발생했는지
- **해결**: 어떻게 해결했는지

---

---

## [TS-001] PMD - Mission.java 매직 넘버 위반 (기존 코드)

- **발생 시점**: 단계 8 완료 후 `./gradlew check` 실행
- **문제**: `pmdMain` 태스크 실패. `Mission.java` 58, 105번 라인의 `content.length() > 4096` 에서 `AvoidLiteralsInIfCondition` 위반
- **원인**: 기존 코드에 `MAX_CONTENT_LENGTH = 4096` 상수가 없이 리터럴을 직접 if 조건에 사용. 구독 도메인 추가 후 처음으로 전체 PMD 검사가 실행되어 발견됨
- **해결**: `Mission.java`에 `private static final int MAX_CONTENT_LENGTH = 4096;` 상수 추가 후 if 조건에서 참조

---

## [TS-002] PMD - SubscriptionControllerTest `UnitTestShouldIncludeAssert` 위반

- **발생 시점**: 단계 8 완료 후 `./gradlew check` 실행
- **문제**: `pmdTest` 태스크 실패. 예외 케이스 테스트 4개에서 `UnitTestShouldIncludeAssert` 위반
- **원인**: MockMvc의 `andExpect()`는 PMD가 assertion으로 인식하지 않음. `CourseControllerTest`는 오류 케이스에도 `verify()` 호출이 있어 통과했으나, `SubscriptionControllerTest`에서 누락
- **해결**: 오류 케이스 테스트 메서드에 `verify(subscriptionService).메서드명(...)` 추가

---

<!-- 구현 진행 중 문제 발생 시 아래에 이어서 기록 -->
