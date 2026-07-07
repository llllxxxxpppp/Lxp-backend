# 아키텍처

## 프로젝트 패키지 구조

전체 프로젝트 구조는 다음을 따른다.

```text
com.lcs.lxp
├─ (Bounded Context)
├─ config
├─ security
└─ common
   ├─ event
   └─ exception
```

- config: Spring 설정 클래스들이 위치한다.
- security: Spring Security, JWT 인증 관련 설정 클래스들이 위치한다.
- common
  - exception
  - event

### 전역 event 패키지

- `ApplicationEventPublisher`와 기반 이벤트 객체 (`BaseDomainEvent`) 를 정의한다.
- 이벤트 객체가 발행될 때 로그를 info 레벨로 기록한다.
- 기반 이벤트는 다음 필드들을 가진다.
  - 이벤트 ID: UUIDv4로 랜덤 생성 및 불변으로 고정한다.
  - 이벤트 발행 일시: `OffsetTimeDate` 로 생성 및 고정한다.

### 전역 exception 패키지

- 도메인 예외 객체를 정의한다.
- 도메인 예외 객체에 대한 전역 예외 핸들러를 정의한다.
- 공통 GlobalExceptionHandler(@RestControllerAdvice)에서 일괄 처리 후 표준 에러 응답으로 변환한다.
- Checked Exception 남용 금지, RuntimeException 기반 통일한다.

## 공통 도메인 패키지 구조

### 공통

바운디드 컨텍스트별 패키지 구조는 다음을 기반으로 한다.

```text
(Bounded Context)
├─ controller
├─ dto
│  ├─ request
│  └─ response
├─ model
│  ├─ entity
│  └─ vo
├─ service
├─ repository
├─ event
└─ exception
```

#### 바운디드 컨텍스트별 event 패키지

- 바운디드 컨텍스트의 모든 이벤트는 이 기반 이벤트 객체를 상속하여 구현해야 한다.
- 이벤트 리스너는 처리 시작 전후에 이벤트 정보 모두를 info 레벨로 로그에 기록한다.

#### 바운디드 컨텍스트별 exception 패키지

- 바운디드 컨텍스트 예외 객체를 정의한다.
- 바운디드 컨텍스트 예외 객체는 전역 도메인 예외 객체를 상속받아야 한다.
- 바운디드 컨텍스트 예외 객체를 이용하여 바운디드 컨텍스트별로 예외 핸들러를 정의한다.

### 구독권 관리 바운디드 컨텍스트

예외적으로 구독권 관리 바운디드 컨텍스트는 다음과 같은 헥사고날 클린 아키텍처 구조를 기반으로 한다.

```text
subscription (Bounded Context)
├─ application
│  ├─ service
│  └─ dto
│     ├─ request
│     └─ response
├─ domain
│  ├─ model
│  │  ├─ entity
│  │  └─ vo
│  ├─ event
│  ├─ repository
│  └─ exception
├─ infrastructure
└─ presentation
```
