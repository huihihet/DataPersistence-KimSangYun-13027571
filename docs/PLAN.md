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
Phase 3  Repository 계층 구현 + 테스트 (SampleRepository + OrderRepository)
Phase 4  Main 시나리오 스크립트 + 빌드 & 커버리지 검증
```

의존 관계: `Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 4`

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

src/test/java/org/ssemi/persistence/model/
├── SampleTest.java   (10개 테스트)
└── OrderTest.java    (10개 테스트)
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
- [ ] `SampleTest` 10개, `OrderTest` 10개 — `./gradlew test` 통과

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
| 11 | `readAll_blankFileContent_returnsEmptyList` | 공백만 있는 파일 → 빈 리스트 |
| 12 | `readAll_emptyArrayContent_returnsEmptyList` | `[]` 내용 파일 → 빈 리스트 |
| 13 | `readAll_validJsonObject_returnsEmptyList` | 유효 JSON이나 배열 아닌 경우 → 빈 리스트 |
| 14 | `writeAll_ioError_throwsUncheckedIOException` | 쓰기 거부 경로 → `UncheckedIOException` |

픽스처: 모든 테스트에 `@TempDir Path tempDir` 주입.

### 완료 조건

- [ ] `JsonFileUtilTest` 14개 테스트 모두 통과
- [ ] `util` 패키지 라인 커버리지 90% 이상

---

## Phase 3 — Repository 계층 구현 + 테스트

**목표**: `SampleRepository`·`OrderRepository` 인터페이스와 `JsonSampleRepository`·`JsonOrderRepository`
구현체를 함께 작성한다. 두 Repository는 구조가 동일하며 `OrderRepository`에 `findByStatus`가 추가된다.

### 생성 파일

```
src/main/java/org/ssemi/persistence/repository/SampleRepository.java
src/main/java/org/ssemi/persistence/repository/JsonSampleRepository.java
src/main/java/org/ssemi/persistence/repository/OrderRepository.java
src/main/java/org/ssemi/persistence/repository/JsonOrderRepository.java
src/test/java/org/ssemi/persistence/repository/JsonSampleRepositoryTest.java
src/test/java/org/ssemi/persistence/repository/JsonOrderRepositoryTest.java
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
| `save` | `loadMutable()`로 로드 → 중복 ID 검사(`IllegalArgumentException`) → 추가 → `JsonFileUtil.writeAll` |
| `findById` | `readAll()` 스트림 필터 → `Optional` 반환 |
| `findAll` | `JsonFileUtil.readAll(filePath, SAMPLE_LIST_TYPE)` |
| `update` | `loadMutable()` → ID 존재 검사(`NoSuchElementException`) → 교체(`set`) → `writeAll` |
| `deleteById` | `loadMutable()` → ID 존재 검사(`NoSuchElementException`) → 제거(`removeIf`) → `writeAll` |
| null 검사 | 각 메서드 진입부 → `IllegalArgumentException` (update/save(null)도 IAE로 통일) |
| `loadMutable()` | `new ArrayList<>(readAll())` — `emptyList()`(수정 불가) 방어용 내부 헬퍼 |

### 3-3. `OrderRepository.java` 인터페이스

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

### 3-4. `JsonOrderRepository.java` 구현 명세

`JsonSampleRepository`와 동일한 구조. 추가 메서드:

| 메서드 | 구현 |
|--------|------|
| `findByStatus(OrderStatus status)` | `status == null` → `IllegalArgumentException`; `findAll()` 스트림 필터 → 리스트 반환 |

### 3-5. `JsonSampleRepositoryTest.java` 테스트 케이스 (13개)

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
| 13 | `update_null_throwsException` | `update(null)` → `IllegalArgumentException` |

### 3-6. `JsonOrderRepositoryTest.java` 테스트 케이스 (16개)

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
| 16 | `update_null_throwsException` | `update(null)` → `IllegalArgumentException` |

픽스처: `@TempDir Path tempDir`; `@BeforeEach`에서 각 Repository를 `tempDir.resolve("파일명.json")`으로 초기화.

### 완료 조건

- [ ] `JsonSampleRepositoryTest` 13개, `JsonOrderRepositoryTest` 16개 — 총 29개 모두 통과
- [ ] `repository` 패키지 라인 커버리지 90% 이상

---

## Phase 4 — Main 시나리오 스크립트 + 빌드 & 커버리지 검증

**목표**: `./gradlew run`으로 CRUD 전 과정을 콘솔에서 시각적으로 확인하고, 전체 빌드와 커버리지 목표를 수치로 검증한다.

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

### 빌드 & 커버리지 검증 명령

```bash
./gradlew build
./gradlew jacocoTestReport
./gradlew jacocoTestCoverageVerification
```

리포트 위치:
```
build/reports/tests/test/index.html
build/reports/jacoco/test/html/index.html
```

### 완료 조건

- [ ] `./gradlew run` 정상 실행, 각 단계 출력 확인
- [ ] `./gradlew build` — BUILD SUCCESSFUL
- [ ] JUnit 전체 테스트(63개) 통과
- [ ] `util`·`repository` 패키지 라인 커버리지 90% 이상
- [ ] PRD.md 섹션 10 완료 조건 전 항목 체크

---

## 파일 생성 순서 요약

```
Phase 0  build.gradle                                     (수정)

Phase 1  model/OrderStatus.java                           (신규)
         model/Sample.java                                (신규)
         model/Order.java                                 (신규)
         test/.../model/SampleTest.java                   (신규, 10개 테스트)
         test/.../model/OrderTest.java                    (신규, 10개 테스트)

Phase 2  util/JsonFileUtil.java                           (신규)
         test/.../util/JsonFileUtilTest.java              (신규, 14개 테스트)

Phase 3  repository/SampleRepository.java                 (신규)
         repository/JsonSampleRepository.java             (신규)
         repository/OrderRepository.java                  (신규)
         repository/JsonOrderRepository.java              (신규)
         test/.../repository/JsonSampleRepositoryTest.java (신규, 13개 테스트)
         test/.../repository/JsonOrderRepositoryTest.java  (신규, 16개 테스트)

Phase 4  Main.java                                        (신규)
         (검증만, 추가 파일 변경 없음)
```

> 테스트 총 합계: model(20) + util(14) + repository(13+16) = **63개**

---

## 리스크 및 주의 사항

| 리스크 | 대응 |
|--------|------|
| Gson이 `enum`을 문자열로 직렬화하지 않을 경우 | `GsonBuilder().registerTypeAdapter(OrderStatus.class, ...)` 추가 |
| `double` 정밀도 손실 (`yield`) | `GsonBuilder().serializeSpecialFloatingPointValues()` 검토 |
| `data/` 경로가 실행 위치에 따라 달라질 경우 | `Main`에서 `Paths.get("data")` 사용; 테스트는 `@TempDir` 격리 |
| Gson 기본 생성자 요구 | `Sample`, `Order` 모두 기본 생성자 필수 |
