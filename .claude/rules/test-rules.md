# 테스트 코드 작성 규칙

테스트 코드 작성 시 다음 규칙을 반드시 준수한다.

## 규칙

- 테스트 코드는 `given[조건]_when[행위]_then[결과]` 의 BDD 패턴으로 영문 작성되어야 한다.
- 테스트 설명은 `@DisplayName` 어노테이션을 사용하여 작성한다.
- 모든 테스트는 반드시 `verify()` 또는 `verifyNoInteractions()` 를 포함해야 한다. (PMD `UnitTestShouldIncludeAssert` 규칙을 만족하기 위해 필요)
