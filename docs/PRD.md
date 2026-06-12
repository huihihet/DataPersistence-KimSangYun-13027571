# DataPersistence PoC — PRD

**버전**: 1.0.0
**작성일**: 2026-06-12
**상태**: 초안

---

## 1. 개요

### 1.1 목적

`SSemi` 본 프로젝트의 JSON 파일 기반 Repository 구현 전략을 사전 검증하는 PoC이다.
`Sample`·`Order` 두 엔티티에 대해 CRUD 전 과정을 JSON 파일로 저장·불러오기하는
구조를 구축하고, 이 패턴을 SSemi에 그대로 이식할 수 있음을 확인한다.

### 1.2 범위

| 포함 | 제외 |
|------|------|
| Repository 인터페이스 + Gson 구현체 | Controller / View 레이어 |
| CRUD 5종 (save / findById / findAll / update / deleteById) | 비즈니스 로직 (재고 계산, 상태 전이) |
| JSON 파일 저장·불러오기 (`data/` 디렉토리) | 콘솔 메뉴 UI |
| 공통 파일 유틸(`JsonFileUtil`) | 네트워크·DB 연동 |
| JUnit 단위 테스트 + JaCoCo 커버리지 | 통합 시나리오 테스트 |

---

## 2. 도메인 모델 (PoC 한정)

### 2.1 Sample (시료)

```java
public class Sample {
    private String sampleId;      // 고유 식별자 (e.g., "S001")
    private String name;          // 시료 이름
    private int avgProductionTime; // 평균 생산 시간 (분)
    private double yield;         // 수율 (0.0 ~ 1.0)
    private int stock;            // 현재 재고 수량
}
```

### 2.2 Order (주문)

```java
public class Order {
    private String orderId;       // 고유 식별자 (e.g., "O001")
    private String sampleId;      // 주문 시료 ID
    private String customerName;  // 고객명
    private int quantity;         // 주문 수량
    private OrderStatus status;   // 주문 상태 (enum)
}
```

### 2.3 OrderStatus (enum)

```java
public enum OrderStatus {
    RESERVED, PRODUCING, CONFIRMED, RELEASE, REJECTED
}
```

---

## 3. 기능 요구사항

### 3.1 SampleRepository

| 메서드 | 설명 | 예외 처리 |
|--------|------|-----------|
| `save(Sample)` | 신규 시료 저장, 중복 ID 시 예외 | `IllegalArgumentException` — 중복 sampleId |
| `findById(String)` | ID로 단건 조회 | `Optional.empty()` — 없을 경우 |
| `findAll()` | 전체 시료 목록 반환 | 빈 리스트 반환 |
| `update(Sample)` | ID 기준 덮어쓰기 | `NoSuchElementException` — 없는 ID |
| `deleteById(String)` | ID 기준 삭제 | `NoSuchElementException` — 없는 ID |

> **null 인자 공통 정책**: 모든 Repository 메서드에서 `null` 인자는 `IllegalArgumentException` 또는 `NullPointerException`을 발생시킨다 (섹션 6.5 참고).

### 3.2 OrderRepository

| 메서드 | 설명 | 예외 처리 |
|--------|------|-----------|
| `save(Order)` | 신규 주문 저장, 중복 ID 시 예외 | `IllegalArgumentException` — 중복 orderId |
| `findById(String)` | ID로 단건 조회 | `Optional.empty()` — 없을 경우 |
| `findAll()` | 전체 주문 목록 반환 | 빈 리스트 반환 |
| `findByStatus(OrderStatus)` | 상태별 주문 목록 반환 | 빈 리스트 반환; `null` 인자 시 `IllegalArgumentException` |
| `update(Order)` | ID 기준 덮어쓰기 | `NoSuchElementException` — 없는 ID |
| `deleteById(String)` | ID 기준 삭제 | `NoSuchElementException` — 없는 ID |

### 3.3 JsonFileUtil

| 메서드 | 설명 |
|--------|------|
| `<T> List<T> readAll(Path, Type)` | JSON 파일 → `List<T>` 역직렬화. 파일 없으면 빈 리스트 반환 |
| `<T> void writeAll(Path, List<T>)` | `List<T>` → JSON 파일 전체 덮어쓰기 (UTF-8) |

---

## 4. 비기능 요구사항

| 항목 | 요구사항 |
|------|---------|
| JSON 라이브러리 | Gson 2.11 이상 |
| 파일 인코딩 | UTF-8 |
| 저장 포맷 | JSON 배열(`[...]`), 들여쓰기 적용 (pretty print) |
| 파일 경로 | `data/samples.json`, `data/orders.json` |
| 초기화 | 파일 없을 경우 빈 배열로 자동 초기화, 예외 미발생 |
| 쓰기 전략 | 전체 목록 덮어쓰기 (append 방식 금지) |
| 테스트 격리 | 테스트용 임시 파일(`@TempDir`) 사용, `data/` 실제 파일 오염 금지 |

---

## 5. JSON 파일 포맷 예시

### `data/samples.json`

```json
[
  {
    "sampleId": "S001",
    "name": "GaN 웨이퍼 A",
    "avgProductionTime": 120,
    "yield": 0.85,
    "stock": 50
  },
  {
    "sampleId": "S002",
    "name": "SiC 기판 B",
    "avgProductionTime": 90,
    "yield": 0.92,
    "stock": 0
  }
]
```

### `data/orders.json`

```json
[
  {
    "orderId": "O001",
    "sampleId": "S001",
    "customerName": "서울대 나노연구소",
    "quantity": 10,
    "status": "RESERVED"
  },
  {
    "orderId": "O002",
    "sampleId": "S002",
    "customerName": "카이스트 반도체랩",
    "quantity": 5,
    "status": "PRODUCING"
  }
]
```

---

## 6. 테스트 전략

### 6.1 JsonFileUtilTest

| 케이스 | 검증 내용 |
|--------|-----------|
| 파일 없을 때 readAll | 빈 리스트 반환, 예외 없음 |
| 객체 저장 후 readAll | 동일 데이터 역직렬화 확인 |
| 빈 리스트 writeAll | 파일에 `[]` 기록 확인 |
| 한글 포함 문자열 | UTF-8 인코딩 손실 없음 확인 |
| 손상된 JSON 파일 readAll | `JsonSyntaxException` 또는 지정 RuntimeException 발생 |
| `data/` 디렉토리 미존재 시 writeAll | 자동 디렉토리 생성 후 파일 기록 성공 |
| `double` 경계값 (0.0, 1.0) 직렬화 | 정밀도 손실 없이 역직렬화 확인 |

### 6.2 JsonSampleRepositoryTest

| 케이스 | 검증 내용 |
|--------|-----------|
| save → findById | 저장 후 동일 객체 반환 |
| save 중복 ID | `IllegalArgumentException` 발생 |
| findAll 초기 | 빈 리스트 |
| update 존재 | 필드 변경 후 findById로 확인 |
| update 없는 ID | `NoSuchElementException` 발생 |
| deleteById 존재 | 삭제 후 findById → empty |
| deleteById 없는 ID | `NoSuchElementException` 발생 |
| 파일 재시작 후 findAll | 영속성 확인 (같은 파일 경로로 재생성) |

### 6.3 JsonOrderRepositoryTest

| 케이스 | 검증 내용 |
|--------|-----------|
| save → findById | 저장 후 동일 객체 반환 |
| findAll 초기 | 빈 리스트 |
| findById 없는 ID | `Optional.empty()` 반환 |
| findByStatus | 상태별 필터링 정확성 |
| findByStatus — 해당 상태 없음 | 빈 리스트 반환 |
| update status | 상태 변경 후 findAll 결과 확인 |
| update 없는 ID | `NoSuchElementException` 발생 |
| deleteById 존재 | 삭제 후 findById → empty |
| deleteById 없는 ID | `NoSuchElementException` 발생 |
| save 중복 ID | `IllegalArgumentException` 발생 |
| 파일 재시작 후 findAll | 영속성 확인 |

### 6.4 테스트 픽스처 전략

- 모든 Repository 테스트는 `@TempDir`으로 임시 디렉토리를 생성해 실제 `data/` 디렉토리를 오염시키지 않는다.
- `@BeforeEach`에서 Repository 인스턴스를 임시 경로로 초기화한다.
- 각 테스트는 독립 상태에서 시작하며 `@TempDir`가 테스트별 격리를 보장한다.

### 6.5 null 인자 정책

- `save(null)`, `update(null)` → `NullPointerException` 또는 `IllegalArgumentException` 발생 (구현 시 선택)
- `findById(null)`, `deleteById(null)` → `IllegalArgumentException` 발생
- `findByStatus(null)` → `IllegalArgumentException` 발생
- 위 정책은 기능 요구사항(섹션 3) 예외 처리 항목에 해당하며 구현 단계에서 동일하게 적용한다.

### 6.6 커버리지 목표

- `repository` 패키지: **90% 이상**
- `util` 패키지: **90% 이상**

---

## 7. 구현 패키지 구조

```
src/main/java/org/ssemi/persistence/
├── Main.java
├── model/
│   ├── Sample.java
│   ├── Order.java
│   └── OrderStatus.java
├── repository/
│   ├── SampleRepository.java          (interface)
│   ├── OrderRepository.java           (interface)
│   ├── JsonSampleRepository.java
│   └── JsonOrderRepository.java
└── util/
    └── JsonFileUtil.java
```

---

## 8. build.gradle 변경 사항

아래 의존성을 `build.gradle`에 추가해야 한다.

```groovy
dependencies {
    implementation 'com.google.code.gson:gson:2.11.0'

    testImplementation platform('org.junit:junit-bom:6.0.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

`application` 플러그인 및 `mainClass` 설정도 추가한다.

```groovy
plugins {
    id 'java'
    id 'application'
}

application {
    mainClass = 'org.ssemi.persistence.Main'
}
```

---

## 9. SSemi 연계 전략

이 PoC에서 확정된 패턴을 SSemi에 이식할 때:

| DataPersistence | SSemi 대응 |
|-----------------|-----------|
| `JsonSampleRepository` | `model/repository/JsonSampleRepository.java` |
| `JsonOrderRepository` | `model/repository/JsonOrderRepository.java` |
| `JsonFileUtil` | `util/JsonFileUtil.java` (공통 유틸) |
| `data/*.json` 경로 전략 | SSemi `data/` 동일 구조 유지 |

---

## 10. 완료 조건

- [ ] `JsonSampleRepository` CRUD 5종 구현 및 테스트 통과
- [ ] `JsonOrderRepository` CRUD 5종 + `findByStatus` 구현 및 테스트 통과
- [ ] `JsonFileUtil` 구현 및 테스트 통과
- [ ] 파일 없을 때 자동 초기화(빈 배열) 동작 확인
- [ ] `data/` 디렉토리 없을 때 `writeAll` 첫 호출 시 자동 생성 동작 확인
- [ ] 손상된 JSON 파일 읽기 시 명시적 예외 발생 확인
- [ ] null 인자 예외 발생 확인
- [ ] 파일 재시작 후 영속성 유지 확인
- [ ] `./gradlew build` 성공
- [ ] JaCoCo 커버리지 repository·util 패키지 90% 이상
