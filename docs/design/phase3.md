# Phase 3 — Repository 계층 설계

**버전**: 1.1.0
**작성일**: 2026-06-12
**기반 문서**: `docs/PLAN.md` Phase 3, `docs/PRD.md` 섹션 3.1·3.2·6.2·6.3·6.4·6.5
**선행 Phase**: Phase 2 (JsonFileUtil) 완료 필요
**상태**: 초안

---

## 목표

`SampleRepository`·`OrderRepository` 인터페이스를 정의하고
`JsonSampleRepository`·`JsonOrderRepository` 구현체를 작성한다.
두 Repository는 구조가 동일하며 `OrderRepository`에 `findByStatus`가 추가된다.
Phase 2에서 검증된 `JsonFileUtil`을 유일한 파일 I/O 창구로 사용한다.

---

## 생성 파일

```
src/main/java/org/ssemi/persistence/repository/SampleRepository.java
src/main/java/org/ssemi/persistence/repository/JsonSampleRepository.java
src/main/java/org/ssemi/persistence/repository/OrderRepository.java
src/main/java/org/ssemi/persistence/repository/JsonOrderRepository.java
src/test/java/org/ssemi/persistence/repository/JsonSampleRepositoryTest.java
src/test/java/org/ssemi/persistence/repository/JsonOrderRepositoryTest.java
```

---

## 1. SampleRepository 인터페이스

### 1.1 파일 정보

| 항목 | 값 |
|------|-----|
| 파일 경로 | `src/main/java/org/ssemi/persistence/repository/SampleRepository.java` |
| 종류 | `interface` |
| 패키지 | `org.ssemi.persistence.repository` |

### 1.2 메서드 명세

| 메서드 | 반환 타입 | 예외 |
|--------|-----------|------|
| `save(Sample sample)` | `void` | `IllegalArgumentException` — null 인자 또는 중복 ID |
| `findById(String sampleId)` | `Optional<Sample>` | `IllegalArgumentException` — null 인자 |
| `findAll()` | `List<Sample>` | — |
| `update(Sample sample)` | `void` | `IllegalArgumentException` — null 인자; `NoSuchElementException` — 없는 ID |
| `deleteById(String sampleId)` | `void` | `IllegalArgumentException` — null 인자; `NoSuchElementException` — 없는 ID |

### 1.3 전체 코드

```java
package org.ssemi.persistence.repository;

import org.ssemi.persistence.model.Sample;

import java.util.List;
import java.util.Optional;

public interface SampleRepository {
    void save(Sample sample);
    Optional<Sample> findById(String sampleId);
    List<Sample> findAll();
    void update(Sample sample);
    void deleteById(String sampleId);
}
```

---

## 2. JsonSampleRepository 구현체

### 2.1 파일 정보

| 항목 | 값 |
|------|-----|
| 파일 경로 | `src/main/java/org/ssemi/persistence/repository/JsonSampleRepository.java` |
| 패키지 | `org.ssemi.persistence.repository` |
| 구현 인터페이스 | `SampleRepository` |

### 2.2 필드 및 생성자

```java
private static final Type SAMPLE_LIST_TYPE =
        new TypeToken<List<Sample>>() {}.getType();

private final Path filePath;

public JsonSampleRepository(Path filePath) {
    this.filePath = filePath;
}
```

파일 경로를 생성자로 주입하는 이유: 테스트 시 `@TempDir` 경로를 넘겨 격리할 수 있다.

### 2.3 null 인자 정책

PRD 6.5에서 `save(null)`·`update(null)` 에 대해 `IllegalArgumentException` 또는 `NullPointerException` 중
하나를 선택하도록 허용하고 있다. 이 PoC에서는 **모든 null 인자를 `IllegalArgumentException`으로 통일**한다.
일관된 오류 타입이 테스트 작성과 호출자 처리를 단순하게 만들기 때문이다.

### 2.4 메서드별 구현 명세

#### `save(Sample sample)`

| 단계 | 조건 | 동작 |
|------|------|------|
| 1 | `sample == null` | `IllegalArgumentException` 발생 |
| 2 | — | `loadMutable()`로 현재 목록 로드 |
| 3 | 동일 `sampleId` 이미 존재 | `IllegalArgumentException` 발생 |
| 4 | — | 목록에 추가 |
| 5 | — | `JsonFileUtil.writeAll(filePath, list)` |

#### `findById(String sampleId)`

| 단계 | 조건 | 동작 |
|------|------|------|
| 1 | `sampleId == null` | `IllegalArgumentException` 발생 |
| 2 | — | `JsonFileUtil.readAll(filePath, SAMPLE_LIST_TYPE)` 스트림 필터 |
| 3 | 일치하는 항목 있음 | `Optional.of(sample)` 반환 |
| 4 | 일치하는 항목 없음 | `Optional.empty()` 반환 |

#### `findAll()`

| 단계 | 동작 |
|------|------|
| 1 | `JsonFileUtil.readAll(filePath, SAMPLE_LIST_TYPE)` 반환 |

빈 파일이거나 파일이 없으면 `JsonFileUtil`이 `emptyList()`를 반환하므로 별도 처리 불필요.

#### `update(Sample sample)`

| 단계 | 조건 | 동작 |
|------|------|------|
| 1 | `sample == null` | `IllegalArgumentException` 발생 |
| 2 | — | `loadMutable()`로 현재 목록 로드 |
| 3 | 동일 `sampleId` 없음 | `NoSuchElementException` 발생 |
| 4 | — | 기존 항목을 `sample`로 교체 (`list.set(idx, sample)`) |
| 5 | — | `JsonFileUtil.writeAll(filePath, list)` |

#### `deleteById(String sampleId)`

| 단계 | 조건 | 동작 |
|------|------|------|
| 1 | `sampleId == null` | `IllegalArgumentException` 발생 |
| 2 | — | `loadMutable()`로 현재 목록 로드 |
| 3 | `removeIf` 결과 `false` (항목 없음) | `NoSuchElementException` 발생 |
| 4 | — | `JsonFileUtil.writeAll(filePath, list)` |

### 2.5 내부 헬퍼: `loadMutable()`

`JsonFileUtil.readAll()`은 파일이 없거나 비어 있을 때 `Collections.emptyList()`(수정 불가)를 반환한다.
`save`·`update`·`deleteById`는 목록을 수정해야 하므로 항상 `ArrayList`가 필요하다.

```java
private List<Sample> loadMutable() {
    return new ArrayList<>(JsonFileUtil.readAll(filePath, SAMPLE_LIST_TYPE));
}
```

`findAll()`과 `findById()`는 읽기 전용이므로 `JsonFileUtil.readAll()` 결과를 직접 사용한다.

### 2.6 전체 코드

```java
package org.ssemi.persistence.repository;

import com.google.gson.reflect.TypeToken;
import org.ssemi.persistence.model.Sample;
import org.ssemi.persistence.util.JsonFileUtil;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class JsonSampleRepository implements SampleRepository {

    private static final Type SAMPLE_LIST_TYPE =
            new TypeToken<List<Sample>>() {}.getType();

    private final Path filePath;

    public JsonSampleRepository(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(Sample sample) {
        if (sample == null) throw new IllegalArgumentException("sample must not be null");
        List<Sample> list = loadMutable();
        if (list.stream().anyMatch(s -> s.getSampleId().equals(sample.getSampleId()))) {
            throw new IllegalArgumentException("Duplicate sampleId: " + sample.getSampleId());
        }
        list.add(sample);
        JsonFileUtil.writeAll(filePath, list);
    }

    @Override
    public Optional<Sample> findById(String sampleId) {
        if (sampleId == null) throw new IllegalArgumentException("sampleId must not be null");
        return JsonFileUtil.readAll(filePath, SAMPLE_LIST_TYPE).stream()
                .filter(s -> s.getSampleId().equals(sampleId))
                .findFirst();
    }

    @Override
    public List<Sample> findAll() {
        return JsonFileUtil.readAll(filePath, SAMPLE_LIST_TYPE);
    }

    @Override
    public void update(Sample sample) {
        if (sample == null) throw new IllegalArgumentException("sample must not be null");
        List<Sample> list = loadMutable();
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getSampleId().equals(sample.getSampleId())) {
                idx = i;
                break;
            }
        }
        if (idx == -1) {
            throw new NoSuchElementException("sampleId not found: " + sample.getSampleId());
        }
        list.set(idx, sample);
        JsonFileUtil.writeAll(filePath, list);
    }

    @Override
    public void deleteById(String sampleId) {
        if (sampleId == null) throw new IllegalArgumentException("sampleId must not be null");
        List<Sample> list = loadMutable();
        boolean removed = list.removeIf(s -> s.getSampleId().equals(sampleId));
        if (!removed) {
            throw new NoSuchElementException("sampleId not found: " + sampleId);
        }
        JsonFileUtil.writeAll(filePath, list);
    }

    private List<Sample> loadMutable() {
        return new ArrayList<>(JsonFileUtil.readAll(filePath, SAMPLE_LIST_TYPE));
    }
}
```

---

## 3. OrderRepository 인터페이스

### 3.1 파일 정보

| 항목 | 값 |
|------|-----|
| 파일 경로 | `src/main/java/org/ssemi/persistence/repository/OrderRepository.java` |
| 종류 | `interface` |
| 패키지 | `org.ssemi.persistence.repository` |

### 3.2 메서드 명세

| 메서드 | 반환 타입 | 예외 |
|--------|-----------|------|
| `save(Order order)` | `void` | `IllegalArgumentException` — null 인자 또는 중복 ID |
| `findById(String orderId)` | `Optional<Order>` | `IllegalArgumentException` — null 인자 |
| `findAll()` | `List<Order>` | — |
| `findByStatus(OrderStatus status)` | `List<Order>` | `IllegalArgumentException` — null 인자 |
| `update(Order order)` | `void` | `IllegalArgumentException` — null 인자; `NoSuchElementException` — 없는 ID |
| `deleteById(String orderId)` | `void` | `IllegalArgumentException` — null 인자; `NoSuchElementException` — 없는 ID |

### 3.3 전체 코드

```java
package org.ssemi.persistence.repository;

import org.ssemi.persistence.model.Order;
import org.ssemi.persistence.model.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String orderId);
    List<Order> findAll();
    List<Order> findByStatus(OrderStatus status);
    void update(Order order);
    void deleteById(String orderId);
}
```

---

## 4. JsonOrderRepository 구현체

### 4.1 파일 정보

| 항목 | 값 |
|------|-----|
| 파일 경로 | `src/main/java/org/ssemi/persistence/repository/JsonOrderRepository.java` |
| 패키지 | `org.ssemi.persistence.repository` |
| 구현 인터페이스 | `OrderRepository` |

### 4.2 JsonSampleRepository와 차이점

`JsonSampleRepository`와 구조가 동일하다. 차이점:

| 항목 | JsonSampleRepository | JsonOrderRepository |
|------|---------------------|---------------------|
| 엔티티 타입 | `Sample` | `Order` |
| ID 필드 메서드 | `getSampleId()` | `getOrderId()` |
| `Type` 상수 | `SAMPLE_LIST_TYPE` | `ORDER_LIST_TYPE` |
| 추가 메서드 | 없음 | `findByStatus(OrderStatus)` |

### 4.3 `findByStatus` 명세

| 단계 | 조건 | 동작 |
|------|------|------|
| 1 | `status == null` | `IllegalArgumentException` 발생 |
| 2 | — | `findAll()` 스트림에서 `status` 일치(`order.getStatus() == status`) 필터 |
| 3 | — | `List<Order>` 반환 (일치 없으면 빈 리스트) |

### 4.4 전체 코드

```java
package org.ssemi.persistence.repository;

import com.google.gson.reflect.TypeToken;
import org.ssemi.persistence.model.Order;
import org.ssemi.persistence.model.OrderStatus;
import org.ssemi.persistence.util.JsonFileUtil;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonOrderRepository implements OrderRepository {

    private static final Type ORDER_LIST_TYPE =
            new TypeToken<List<Order>>() {}.getType();

    private final Path filePath;

    public JsonOrderRepository(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(Order order) {
        if (order == null) throw new IllegalArgumentException("order must not be null");
        List<Order> list = loadMutable();
        if (list.stream().anyMatch(o -> o.getOrderId().equals(order.getOrderId()))) {
            throw new IllegalArgumentException("Duplicate orderId: " + order.getOrderId());
        }
        list.add(order);
        JsonFileUtil.writeAll(filePath, list);
    }

    @Override
    public Optional<Order> findById(String orderId) {
        if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
        return JsonFileUtil.readAll(filePath, ORDER_LIST_TYPE).stream()
                .filter(o -> o.getOrderId().equals(orderId))
                .findFirst();
    }

    @Override
    public List<Order> findAll() {
        return JsonFileUtil.readAll(filePath, ORDER_LIST_TYPE);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        if (status == null) throw new IllegalArgumentException("status must not be null");
        return findAll().stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public void update(Order order) {
        if (order == null) throw new IllegalArgumentException("order must not be null");
        List<Order> list = loadMutable();
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getOrderId().equals(order.getOrderId())) {
                idx = i;
                break;
            }
        }
        if (idx == -1) {
            throw new NoSuchElementException("orderId not found: " + order.getOrderId());
        }
        list.set(idx, order);
        JsonFileUtil.writeAll(filePath, list);
    }

    @Override
    public void deleteById(String orderId) {
        if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
        List<Order> list = loadMutable();
        boolean removed = list.removeIf(o -> o.getOrderId().equals(orderId));
        if (!removed) {
            throw new NoSuchElementException("orderId not found: " + orderId);
        }
        JsonFileUtil.writeAll(filePath, list);
    }

    private List<Order> loadMutable() {
        return new ArrayList<>(JsonFileUtil.readAll(filePath, ORDER_LIST_TYPE));
    }
}
```

---

## 5. JsonSampleRepositoryTest

### 5.1 파일 정보

| 항목 | 값 |
|------|-----|
| 파일 경로 | `src/test/java/org/ssemi/persistence/repository/JsonSampleRepositoryTest.java` |
| 패키지 | `org.ssemi.persistence.repository` |
| 테스트 프레임워크 | JUnit Jupiter 6.x |

### 5.2 픽스처 전략

```java
@TempDir
Path tempDir;

private JsonSampleRepository repo;

@BeforeEach
void setUp() {
    repo = new JsonSampleRepository(tempDir.resolve("samples.json"));
}
```

### 5.3 테스트 케이스 (13개)

| # | 테스트명 | 검증 내용 | 핵심 assert |
|---|---------|-----------|------------|
| 1 | `save_thenFindById_returnsSample` | 저장 후 동일 객체 반환 | `assertEquals(s, repo.findById("S001").orElseThrow())` |
| 2 | `save_duplicateId_throwsException` | 중복 ID → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| 3 | `findAll_initiallyEmpty` | 초기 빈 리스트 | `assertTrue(repo.findAll().isEmpty())` |
| 4 | `findById_notExists_returnsEmpty` | 없는 ID → `Optional.empty()` | `assertTrue(repo.findById("X").isEmpty())` |
| 5 | `update_existing_fieldsChanged` | stock 변경 후 `findById`로 확인 | `assertEquals(999, found.getStock())` |
| 6 | `update_notExists_throwsException` | 없는 ID → `NoSuchElementException` | `assertThrows(NoSuchElementException.class, ...)` |
| 7 | `deleteById_existing_removedFromList` | 삭제 후 `findById` → empty | `assertTrue(repo.findById("S001").isEmpty())` |
| 8 | `deleteById_notExists_throwsException` | 없는 ID → `NoSuchElementException` | `assertThrows(NoSuchElementException.class, ...)` |
| 9 | `persistence_afterReopen_dataPreserved` | 새 인스턴스로 같은 파일 재오픈 | `assertEquals(1, repo2.findAll().size())` |
| 10 | `save_null_throwsException` | `save(null)` → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| 11 | `findById_null_throwsException` | `findById(null)` → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| 12 | `deleteById_null_throwsException` | `deleteById(null)` → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| 13 | `update_null_throwsException` | `update(null)` → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |

### 5.4 주요 시나리오

**#5 `update_existing_fieldsChanged`**
```
repo.save(new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 50))
repo.update(new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 999))
Sample found = repo.findById("S001").orElseThrow()
→ found.getStock() == 999
```

**#9 `persistence_afterReopen_dataPreserved`**
```
Path path = tempDir.resolve("samples.json")
new JsonSampleRepository(path).save(new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 50))

JsonSampleRepository repo2 = new JsonSampleRepository(path)
→ repo2.findAll().size() == 1
→ repo2.findById("S001").isPresent() == true
```

---

## 6. JsonOrderRepositoryTest

### 6.1 파일 정보

| 항목 | 값 |
|------|-----|
| 파일 경로 | `src/test/java/org/ssemi/persistence/repository/JsonOrderRepositoryTest.java` |
| 패키지 | `org.ssemi.persistence.repository` |
| 테스트 프레임워크 | JUnit Jupiter 6.x |

### 6.2 픽스처 전략

```java
@TempDir
Path tempDir;

private JsonOrderRepository repo;

@BeforeEach
void setUp() {
    repo = new JsonOrderRepository(tempDir.resolve("orders.json"));
}
```

### 6.3 테스트 케이스 (16개)

| # | 테스트명 | 검증 내용 | 핵심 assert |
|---|---------|-----------|------------|
| 1 | `save_thenFindById_returnsOrder` | 저장 후 동일 객체 반환 | `assertEquals(o, repo.findById("O001").orElseThrow())` |
| 2 | `findAll_initiallyEmpty` | 초기 빈 리스트 | `assertTrue(repo.findAll().isEmpty())` |
| 3 | `findById_notExists_returnsEmpty` | 없는 ID → `Optional.empty()` | `assertTrue(repo.findById("X").isEmpty())` |
| 4 | `findByStatus_returnsFilteredList` | RESERVED 2개 저장 후 필터 | `assertEquals(2, result.size())` |
| 5 | `findByStatus_noMatch_returnsEmpty` | 해당 상태 없음 → 빈 리스트 | `assertTrue(result.isEmpty())` |
| 6 | `findByStatus_null_throwsException` | `null` → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| 7 | `update_status_changesReflected` | 상태 변경 후 `findAll` 확인 | `assertEquals(CONFIRMED, repo.findById("O001").get().getStatus())` |
| 8 | `update_notExists_throwsException` | 없는 ID → `NoSuchElementException` | `assertThrows(NoSuchElementException.class, ...)` |
| 9 | `deleteById_existing_removedFromList` | 삭제 후 `findById` → empty | `assertTrue(repo.findById("O001").isEmpty())` |
| 10 | `deleteById_notExists_throwsException` | 없는 ID → `NoSuchElementException` | `assertThrows(NoSuchElementException.class, ...)` |
| 11 | `save_duplicateId_throwsException` | 중복 ID → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| 12 | `persistence_afterReopen_dataPreserved` | 파일 재시작 후 영속성 확인 | `assertEquals(1, repo2.findAll().size())` |
| 13 | `save_null_throwsException` | `save(null)` → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| 14 | `findById_null_throwsException` | `findById(null)` → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| 15 | `deleteById_null_throwsException` | `deleteById(null)` → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| 16 | `update_null_throwsException` | `update(null)` → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |

### 6.4 주요 시나리오

**#4 `findByStatus_returnsFilteredList`**
```
repo.save(new Order("O001", "S001", "서울대", 10, OrderStatus.RESERVED))
repo.save(new Order("O002", "S001", "카이스트", 5, OrderStatus.PRODUCING))
repo.save(new Order("O003", "S002", "연세대", 3, OrderStatus.RESERVED))
List<Order> result = repo.findByStatus(OrderStatus.RESERVED)
→ result.size() == 2
→ result에 O001, O003 포함
```

**#7 `update_status_changesReflected`**
```
repo.save(new Order("O001", "S001", "서울대", 10, OrderStatus.RESERVED))
Order updated = new Order("O001", "S001", "서울대", 10, OrderStatus.CONFIRMED)
repo.update(updated)
→ repo.findById("O001").get().getStatus() == OrderStatus.CONFIRMED
```

---

## 7. 예외 처리 전략 (공통)

| 상황 | 예외 | 발생 위치 |
|------|------|-----------|
| null 인자 (모든 메서드) | `IllegalArgumentException` | 각 메서드 진입부 |
| 중복 ID `save` | `IllegalArgumentException` | 중복 검사 후 |
| 없는 ID `update` | `NoSuchElementException` | 검색 결과 `-1` 후 |
| 없는 ID `deleteById` | `NoSuchElementException` | `removeIf` 결과 `false` 후 |
| 파일 I/O 오류 | `UncheckedIOException` | `JsonFileUtil` 내부에서 래핑 전파 |

---

## 8. 완료 조건

- [ ] `SampleRepository.java`, `JsonSampleRepository.java` 생성
- [ ] `OrderRepository.java`, `JsonOrderRepository.java` 생성
- [ ] `JsonSampleRepositoryTest.java` 생성 — 13개 테스트 모두 통과
- [ ] `JsonOrderRepositoryTest.java` 생성 — 16개 테스트 모두 통과
- [ ] `./gradlew test` — Phase 1(20개) + Phase 2(14개) + Phase 3(29개) = **63개** 전체 통과
- [ ] `repository` 패키지 라인 커버리지 90% 이상 (`./gradlew jacocoTestReport`)

---

## 9. 다음 Phase 연계

Phase 4(`Main.java`)는 이 Phase에서 완성된 두 Repository를 직접 호출해 CRUD 시나리오를 시연한다.
Phase 3 완료 조건이 모두 충족된 후 Phase 4를 시작한다.
