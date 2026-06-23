# 구현 코드 작성 규칙

코드 구현 시 코드 품질을 위해 다음을 반드시 준수할 것.

## 규칙

- Controller는 `@RestController`를 이용하여 구현한다.
- 모든 Service 클래스는 상위 계층과 데이터를 주고받을 때 반드시 DTO를 이용하도록 구현한다.
- DTO와 Entity를 변환하는 책임은 DTO가 `from`, `toEntity` 메서드를 갖도록 구현한다.
