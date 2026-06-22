# 아키텍처

## 프로젝트 패키지 구조

전체 프로젝트 구조는 다음을 따른다.

```text
com.lcs.lxp
├─ (Bounded Context)
├─ config
└─ common
   ├─ event
   └─ exception
```

- config: Spring 설정 클래스들이 위치한다.
- common
  - event: `ApplicationEventPublisher`와 이벤트 기반 객체를 정의한다. 바운디드 컨텍스트의 모든 이벤트는 이 기반 객체를 상속하여 구현해야 한다.
  - exception: 도메인 예외 객체를 정의한다. 바운디드 컨텍스트의 모든 예외는 이 예외 객체를 상속받아야 한다.

## 공통 도메인 패키지 구조

바운디드 컨텍스트별 패키지 구조는 다음을 기반으로 한다.

```text
(Bounded Context)
├─ controller
├─ model
├─ service
├─ repository
├─ event
└─ exception
```

예외적으로 구독권 관리 바운디드 컨텍스트는 다음과 같은 헥사고날 클린 아키텍처 구조를 기반으로 한다.

```text
subscription (Bounded Context)
├─ application
│  ├─ service
│  └─ model
│     └─ dto
├─ domain
│  ├─ model
│  ├─ event
│  ├─ repository
│  └─ exception
└─ infrastructure
```
