# DataPersistence PoC — PLAN

**버전**: 1.0.0
**작성일**: 2026-06-12
**기반 문서**: `docs/PRD.md` v1.0.0
**상태**: 초안

---

## 전체 구조

```
Phase 0  Gradle 설정
Phase 1  도메인 모델 구현
Phase 2  JsonFileUtil 구현 + 테스트
Phase 3  SampleRepository 구현 + 테스트
Phase 4  OrderRepository 구현 + 테스트
Phase 5  Main 시나리오 스크립트
Phase 6  빌드 & 커버리지 검증
```

의존 관계: `Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6`

---

## Phase 0 — Gradle 설정

**목표**: 빌드 환경과 외부 의존성을 확정한다.

### 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `build.gradle` | `application` 플러그인 추가, Gson 2.11.0 의존성 추가, JaCoCo 플러그인 추가 |

### 최종 `build.gradle`

```groovy
plugins {
    id 'java'
    id 'application'
    id 'jacoco'
}

group = 'org.ssemi'
version = '1.0-SNAPSHOT'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass = 'org.ssemi.persistence.Main'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.google.code.gson:gson:2.11.0'

    testImplementation platform('org.junit:junit-bom:6.0.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.90
            }
        }
    }
}
```

### 완료 조건

- [ ] `./gradlew dependencies` — Gson 2.11.0 resolve 확인
- [ ] `./gradlew compileJava` — 오류 없음

---

## Phase 1 — 도메인 모델 구현

**목표**: JSON 직렬화·역직렬화 대상 엔티티 클래스를 작성한다.

### 생성 파일

```
src/main/java/org/ssemi/persistence/model/
├── OrderStatus.java
├── Sample.java
└── Order.java
```

### 1-1. `OrderStatus.java`

```java
package org.ssemi.persistence.model;

public enum OrderStatus {
    RESERVED, PRODUCING, CONFIRMED, RELEASE, REJECTED
}
```

### 1-2. `Sample.java`

| 필드 | 타입 | 설명 |
|------|------|------|
| `sampleId` | `String` | 고유 식별자 |
| `name` | `String` | 시료 이름 |
| `avgProductionTime` | `int` | 평균 생산 시간(분) |
| `yield` | `double` | 수율 (0.0 ~ 1.0) |
| `stock` | `int` | 현재 재고 수량 |

구현 규칙:
- 기본 생성자(Gson 역직렬화용) + 전체 인자 생성자
- `getter` 전체, `setter` 전체
- `equals` / `hashCode`: `sampleId` 기준

### 1-3. `Order.java`

| 필드 | 타입 | 설명 |
|------|------|------|
| `orderId` | `String` | 고유 식별자 |
| `sampleId` | `String` | 주문 시료 ID |
| `customerName` | `String` | 고객명 |
| `quantity` | `int` | 주문 수량 |
| `status` | `OrderStatus` | 주문 상태 |

구현 규칙:
- 기본 생성자(Gson 역직렬화용) + 전체 인자 생성자
- `getter` 전체, `setter` 전체
- `equals` / `hashCode`: `orderId` 기준

### 완료 조건

- [ ] `./gradlew compileJava` — 오류 없음
- [ ] 기본 생성자로 인스턴스 생성 후 setter/getter 동작 확인 (수동 또는 테스트)

---

## Phase 2 — JsonFileUtil 구현 + 테스트

**목표**: 파일 읽기·쓰기 공통 유틸을 구현하고 단위 테스트로 검증한다.
이후 Repository 구현체가 이 유틸에 의존하므로 가장 먼저 안정화한다.

### 생성 파일

```
src/main/java/org/ssemi/persistence/util/JsonFileUtil.java
src/test/java/org/ssemi/persistence/util/JsonFileUtilTest.java
```

### 2-1. `JsonFileUtil.java` 구현 명세

```java
package org.ssemi.persistence.util;

public class JsonFileUtil {
    public static <T> List<T> readAll(Path filePath, Type listType)
    public static <T> void writeAll(Path filePath, List<T> items)
}
```

**`readAll` 동작 명세**:
1. `filePath`가 `null`이면 `IllegalArgumentException`
2. 파일이 존재하지 않으면 `Collections.emptyList()` 반환
3. 파일 내용이 빈 문자열이거나 `"[]"`이면 빈 리스트 반환
4. JSON 파싱 실패 시 `JsonSyntaxException` 그대로 전파 (래핑 금지)
5. 파일 읽기: `Files.readString(filePath, StandardCharsets.UTF_8)`

**`writeAll` 동작 명세**:
1. `filePath`가 `null`이면 `IllegalArgumentException`
2. `items`가 `null`이면 `IllegalArgumentException`
3. 부모 디렉토리가 없으면 `Files.createDirectories(filePath.getParent())` 호출 후 생성
4. 파일 쓰기: `Files.writeString(filePath, gson.toJson(items), StandardCharsets.UTF_8)`
5. 전체 목록 덮어쓰기 (append 금지)

### 2-2. `JsonFileUtilTest.java` 테스트 케이스

| # | 테스트명 | 검증 내용 |
|---|---------|-----------|
| 1 | `readAll_fileNotExists_returnsEmptyList` | 파일 없을 때 빈 리스트, 예외 없음 |
| 2 | `writeAll_thenReadAll_returnsSameData` | 저장 후 동일 데이터 역직렬화 확인 |
| 3 | `writeAll_emptyList_writesEmptyArray` | 빈 리스트 → 파일에 `[]` 기록 |
| 4 | `writeAll_koreanString_utf8Preserved` | 한글 문자열 UTF-8 손실 없음 |
| 5 | `readAll_corruptedJson_throwsException` | 손상 JSON → `JsonSyntaxException` 발생 |
| 6 | `writeAll_dirNotExists_createsDir` | 디렉토리 미존재 시 자동 생성 후 성공 |
| 7 | `writeAll_doubleEdgeValues_noLoss` | `yield=0.0`, `yield=1.0` 정밀도 손실 없음 |
| 8 | `readAll_nullPath_throwsException` | `null` 경로 → `IllegalArgumentException` |
| 9 | `writeAll_nullPath_throwsException` | `null` 경로 → `IllegalArgumentException` |
| 10 | `writeAll_nullItems_throwsException` | `null` 리스트 → `IllegalArgumentException` |

픽스처: 모든 테스트에 `@TempDir Path tempDir` 주입.

### 완료 조건

- [ ] `JsonFileUtilTest` 10개 테스트 모두 통과
- [ ] `util` 패키지 라인 커버리지 90% 이상

---

## Phase 3 — SampleRepository 구현 + 테스트

**목표**: `SampleRepository` 인터페이스를 정의하고 `JsonSampleRepository` 구현체를 작성한다.

### 생성 파일

```
src/main/java/org/ssemi/persistence/repository/SampleRepository.java
src/main/java/org/ssemi/persistence/repository/JsonSampleRepository.java
src/test/java/org/ssemi/persistence/repository/JsonSampleRepositoryTest.java
```

### 3-1. `SampleRepository.java` 인터페이스

```java
package org.ssemi.persistence.repository;

public interface SampleRepository {
    void save(Sample sample);
    Optional<Sample> findById(String sampleId);
    List<Sample> findAll();
    void update(Sample sample);
    void deleteById(String sampleId);
}
```

### 3-2. `JsonSampleRepository.java` 구현 명세

| 항목 | 내용 |
|------|------|
| 생성자 | `JsonSampleRepository(Path filePath)` — 파일 경로를 외부에서 주입 (테스트 격리용) |
| `save` | `findAll()`로 로드 → 중복 ID 검사(`IllegalArgumentException`) → 추가 → `JsonFileUtil.writeAll` |
| `findById` | `findAll()` 스트림 필터 → `Optional` 반환 |
| `findAll` | `JsonFileUtil.readAll(filePath, sampleListType)` |
| `update` | `findAll()` → ID 존재 검사(`NoSuchElementException`) → 교체 → `writeAll` |
| `deleteById` | `findAll()` → ID 존재 검사(`NoSuchElementException`) → 제거 → `writeAll` |
| null 검사 | 각 메서드 진입부에서 인자 null 여부 확인 → `IllegalArgumentException` |

`Type` 획득:
```java
private static final Type SAMPLE_LIST_TYPE =
    new TypeToken<List<Sample>>() {}.getType();
```

### 3-3. `JsonSampleRepositoryTest.java` 테스트 케이스

| # | 테스트명 | 검증 내용 |
|---|---------|-----------|
| 1 | `save_thenFindById_returnsSample` | 저장 후 동일 객체 반환 |
| 2 | `save_duplicateId_throwsException` | 중복 ID → `IllegalArgumentException` |
| 3 | `findAll_initiallyEmpty` | 초기 빈 리스트 |
| 4 | `findById_notExists_returnsEmpty` | 없는 ID → `Optional.empty()` |
| 5 | `update_existing_fieldsChanged` | 필드 변경 후 `findById`로 확인 |
| 6 | `update_notExists_throwsException` | 없는 ID → `NoSuchElementException` |
| 7 | `deleteById_existing_removedFromList` | 삭제 후 `findById` → `empty` |
| 8 | `deleteById_notExists_throwsException` | 없는 ID → `NoSuchElementException` |
| 9 | `persistence_afterReopen_dataPreserved` | 같은 경로로 새 인스턴스 생성 후 `findAll` 동일 결과 |
| 10 | `save_null_throwsException` | `save(null)` → `IllegalArgumentException` |
| 11 | `findById_null_throwsException` | `findById(null)` → `IllegalArgumentException` |
| 12 | `deleteById_null_throwsException` | `deleteById(null)` → `IllegalArgumentException` |
| 13 | `update_null_throwsException` | `update(null)` → `IllegalArgumentException` 또는 `NullPointerException` |

픽스처: `@TempDir Path tempDir`; `@BeforeEach`에서 `new JsonSampleRepository(tempDir.resolve("samples.json"))`.

### 완료 조건

- [ ] `JsonSampleRepositoryTest` 13개 테스트 모두 통과
- [ ] 영속성 테스트(#9) — 파일 재시작 시나리오 통과

---

## Phase 4 — OrderRepository 구현 + 테스트

**목표**: `OrderRepository` 인터페이스를 정의하고 `JsonOrderRepository` 구현체를 작성한다.
Phase 3과 구조가 동일하며, `findByStatus` 메서드가 추가된다.

### 생성 파일

```
src/main/java/org/ssemi/persistence/repository/OrderRepository.java
src/main/java/org/ssemi/persistence/repository/JsonOrderRepository.java
src/test/java/org/ssemi/persistence/repository/JsonOrderRepositoryTest.java
```

### 4-1. `OrderRepository.java` 인터페이스

```java
package org.ssemi.persistence.repository;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String orderId);
    List<Order> findAll();
    List<Order> findByStatus(OrderStatus status);
    void update(Order order);
    void deleteById(String orderId);
}
```

### 4-2. `JsonOrderRepository.java` 구현 명세

Phase 3과 동일한 구조. 추가 메서드:

| 메서드 | 구현 |
|--------|------|
| `findByStatus(OrderStatus status)` | `status == null` → `IllegalArgumentException`; `findAll()` 스트림에서 `status` 일치 필터 → 리스트 반환 |

`Type` 획득:
```java
private static final Type ORDER_LIST_TYPE =
    new TypeToken<List<Order>>() {}.getType();
```

### 4-3. `JsonOrderRepositoryTest.java` 테스트 케이스

| # | 테스트명 | 검증 내용 |
|---|---------|-----------|
| 1 | `save_thenFindById_returnsOrder` | 저장 후 동일 객체 반환 |
| 2 | `findAll_initiallyEmpty` | 초기 빈 리스트 |
| 3 | `findById_notExists_returnsEmpty` | 없는 ID → `Optional.empty()` |
| 4 | `findByStatus_returnsFilteredList` | 상태별 필터링 정확성 |
| 5 | `findByStatus_noMatch_returnsEmpty` | 해당 상태 없음 → 빈 리스트 |
| 6 | `findByStatus_null_throwsException` | `null` → `IllegalArgumentException` |
| 7 | `update_status_changesReflected` | 상태 변경 후 `findAll` 결과 확인 |
| 8 | `update_notExists_throwsException` | 없는 ID → `NoSuchElementException` |
| 9 | `deleteById_existing_removedFromList` | 삭제 후 `findById` → `empty` |
| 10 | `deleteById_notExists_throwsException` | 없는 ID → `NoSuchElementException` |
| 11 | `save_duplicateId_throwsException` | 중복 ID → `IllegalArgumentException` |
| 12 | `persistence_afterReopen_dataPreserved` | 파일 재시작 후 영속성 확인 |
| 13 | `save_null_throwsException` | `save(null)` → `IllegalArgumentException` |
| 14 | `findById_null_throwsException` | `findById(null)` → `IllegalArgumentException` |
| 15 | `deleteById_null_throwsException` | `deleteById(null)` → `IllegalArgumentException` |
| 16 | `update_null_throwsException` | `update(null)` → `IllegalArgumentException` 또는 `NullPointerException` |

픽스처: `@TempDir Path tempDir`; `@BeforeEach`에서 `new JsonOrderRepository(tempDir.resolve("orders.json"))`.

### 완료 조건

- [ ] `JsonOrderRepositoryTest` 16개 테스트 모두 통과
- [ ] `repository` 패키지 라인 커버리지 90% 이상

---

## Phase 5 — Main 시나리오 스크립트

**목표**: `./gradlew run`으로 CRUD 전 과정을 콘솔에서 시각적으로 확인한다.
비즈니스 로직 없이 Repository 호출 흐름만 시연한다.

### 생성 파일

```
src/main/java/org/ssemi/persistence/Main.java
```

### 시나리오 순서

```
1. JsonSampleRepository, JsonOrderRepository 초기화 (data/ 경로)
2. Sample 3개 save (S001, S002, S003)
3. findAll → 콘솔 출력
4. findById("S002") → 출력
5. update S002 (stock 변경)
6. findById("S002") → 변경 확인 출력
7. deleteById("S003")
8. findAll → S003 제거 확인

9. Order 3개 save (O001: RESERVED, O002: PRODUCING, O003: CONFIRMED)
10. findAll → 콘솔 출력
11. findByStatus(RESERVED) → O001만 출력
12. update O001 status → CONFIRMED
13. findByStatus(RESERVED) → 빈 리스트 출력
14. deleteById("O002")
15. findAll → O002 제거 확인

16. 프로그램 재실행 시 data/*.json에서 데이터 복원됨을 안내 메시지 출력
```

### 완료 조건

- [ ] `./gradlew run` 정상 실행, 각 단계 출력 확인
- [ ] `data/samples.json`, `data/orders.json` 파일 생성 확인

---

## Phase 6 — 빌드 & 커버리지 검증

**목표**: 전체 빌드 통과 및 커버리지 목표(90%)를 수치로 확인한다.

### 실행 명령

```bash
# 전체 빌드 (컴파일 + 테스트 + JaCoCo 리포트)
./gradlew build

# 커버리지 리포트 단독 생성
./gradlew jacocoTestReport

# 커버리지 임계값 검증 (90% 미만 시 빌드 실패)
./gradlew jacocoTestCoverageVerification
```

### 리포트 위치

```
build/reports/tests/test/index.html     ← JUnit 테스트 결과
build/reports/jacoco/test/html/index.html ← JaCoCo 커버리지
```

### 완료 조건

- [ ] `./gradlew build` — BUILD SUCCESSFUL
- [ ] JUnit 전체 테스트(39개 이상) 통과
- [ ] `util` 패키지 라인 커버리지 90% 이상
- [ ] `repository` 패키지 라인 커버리지 90% 이상
- [ ] PRD.md 섹션 10 완료 조건 전 항목 체크

---

## 파일 생성 순서 요약

```
Phase 0  build.gradle                                     (수정)

Phase 1  model/OrderStatus.java                           (신규)
         model/Sample.java                                (신규)
         model/Order.java                                 (신규)

Phase 2  util/JsonFileUtil.java                            (신규)
         test/.../util/JsonFileUtilTest.java               (신규, 10개 테스트)

Phase 3  repository/SampleRepository.java                  (신규)
         repository/JsonSampleRepository.java              (신규)
         test/.../repository/JsonSampleRepositoryTest.java (신규, 13개 테스트)

Phase 4  repository/OrderRepository.java                   (신규)
         repository/JsonOrderRepository.java               (신규)
         test/.../repository/JsonOrderRepositoryTest.java  (신규, 16개 테스트)

Phase 5  Main.java                                        (신규)

Phase 6  (검증만, 파일 변경 없음)
```

---

## 리스크 및 주의 사항

| 리스크 | 대응 |
|--------|------|
| Gson이 `enum`을 문자열로 직렬화하지 않을 경우 | `GsonBuilder().registerTypeAdapter(OrderStatus.class, ...)` 추가 |
| `double` 정밀도 손실 (`yield`) | `GsonBuilder().serializeSpecialFloatingPointValues()` 검토 |
| `data/` 경로가 실행 위치에 따라 달라질 경우 | `Main`에서 `Paths.get("data")` 사용; 테스트는 `@TempDir` 격리 |
| Gson 기본 생성자 요구 | `Sample`, `Order` 모두 기본 생성자 필수 |
