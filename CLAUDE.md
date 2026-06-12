# DataPersistence PoC — CLAUDE.md

> 루트 `과제/CLAUDE.md`를 상속한다. 이 파일은 DataPersistence에 특화된 추가 규칙만 기술한다.

---

## PoC 목적

**JSON 파일 영속성 구조 검증** — `SSemi` 본 프로젝트의 `JsonSampleRepository`,
`JsonOrderRepository`, `JsonProductionQueueRepository` 구현 전략을 이 PoC에서 확정한다.

증명 목표:
1. Repository 인터페이스 → JSON 구현체 분리 패턴이 실제 동작함을 확인
2. CRUD(Create / Read / Update / Delete) + 파일 저장·불러오기가 올바르게 작동함을 확인
3. JSON 라이브러리(Gson) 직렬화·역직렬화 전략을 확정

---

## 패키지 구조

```
src/main/java/org/ssemi/persistence/
├── Main.java                          ← CRUD 시나리오 실행 진입점
├── model/
│   ├── Sample.java                    ← 시료 엔티티 (PoC 대상)
│   ├── Order.java                     ← 주문 엔티티 (PoC 대상)
│   └── OrderStatus.java               ← 주문 상태 enum
├── repository/
│   ├── SampleRepository.java          ← CRUD 인터페이스
│   ├── OrderRepository.java           ← CRUD 인터페이스
│   ├── JsonSampleRepository.java      ← Gson 기반 파일 구현체
│   └── JsonOrderRepository.java       ← Gson 기반 파일 구현체
└── util/
    └── JsonFileUtil.java              ← 공통 파일 읽기/쓰기 유틸
```

```
src/test/java/org/ssemi/persistence/
├── repository/
│   ├── JsonSampleRepositoryTest.java
│   └── JsonOrderRepositoryTest.java
└── util/
    └── JsonFileUtilTest.java
```

```
data/                                  ← 런타임 JSON 파일 저장 경로 (gitignore)
├── samples.json
└── orders.json
```

---

## 기술 스택 (PoC 한정)

| 항목 | 선택 | 비고 |
|------|------|------|
| JSON 라이브러리 | **Gson 2.11+** | SSemi 본 프로젝트와 동일 라이브러리 사용 |
| 테스트 | JUnit Jupiter 6.x | `build.gradle` 기존 설정 유지 |
| 커버리지 | JaCoCo | Repository·Util 핵심 로직 90% 이상 |

---

## Repository 인터페이스 규칙

```java
// 모든 Repository가 공통으로 구현해야 할 최소 CRUD 시그니처
void save(T entity);
Optional<T> findById(ID id);
List<T> findAll();
void update(T entity);
void deleteById(ID id);
```

도메인 특화 조회 메서드(e.g., `OrderRepository.findByStatus`)는 인터페이스별로 추가 정의할 수 있다.

---

## JSON 파일 관리 규칙

- 파일 경로: `data/{엔티티복수명소문자}.json` (e.g., `data/samples.json`)
- 포맷: `List<T>` 직렬화 — 최상위가 JSON 배열(`[...]`)
- 파일 없을 경우: 빈 배열(`[]`)로 초기화, 예외 미발생
- 파일 쓰기: 항상 전체 목록을 덮어쓰기 (append 금지)
- 인코딩: UTF-8

---

## 스코프 제한

이 PoC는 **영속성 계층만** 검증한다. 아래는 포함하지 않는다:

- Controller / View 레이어
- 비즈니스 로직 (재고 계산, 상태 전이 등)
- 콘솔 메뉴 UI

`Main.java`는 시나리오 실행용 스크립트 수준으로만 작성한다.

---

## 빌드 & 실행

```bash
# 빌드
./gradlew build

# Main 실행 (CRUD 시나리오 확인)
./gradlew run

# 테스트
./gradlew test

# 커버리지 리포트
./gradlew jacocoTestReport
```
