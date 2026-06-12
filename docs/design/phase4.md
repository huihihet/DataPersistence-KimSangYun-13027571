# Phase 4 — Main 시나리오 스크립트 + 빌드 & 커버리지 검증 설계

**버전**: 1.0.0
**작성일**: 2026-06-12
**기반 문서**: `docs/PLAN.md` Phase 4, `docs/PRD.md` 섹션 5·10, `DataPersistence/CLAUDE.md` 스코프 제한
**선행 Phase**: Phase 3 (Repository 계층) 완료 필요
**상태**: 초안

---

## 목표

`./gradlew run`으로 `JsonSampleRepository`·`JsonOrderRepository`의 CRUD 전 과정을
콘솔에서 시각적으로 확인한다. 이후 전체 빌드와 JaCoCo 커버리지 목표(90%)를 수치로 검증한다.

`Main.java`는 시나리오 실행용 스크립트 수준으로만 작성하며, 비즈니스 로직·Controller·View를 포함하지 않는다.

---

## 생성 파일

```
src/main/java/org/ssemi/persistence/Main.java
```

---

## 1. Main.java

### 1.1 파일 정보

| 항목 | 값 |
|------|-----|
| 파일 경로 | `src/main/java/org/ssemi/persistence/Main.java` |
| 패키지 | `org.ssemi.persistence` |
| 역할 | CRUD 시나리오 실행 스크립트 — 콘솔 출력으로 각 단계 결과를 확인 |

### 1.2 data/ 경로 전략

`Paths.get("data")` 를 사용한다. `./gradlew run` 실행 시 작업 디렉토리는 프로젝트 루트
(`DataPersistence/`)이므로 `data/samples.json`·`data/orders.json`이 프로젝트 루트 하위에 생성된다.

`JsonFileUtil.writeAll()`이 부모 디렉토리(`data/`)가 없으면 자동 생성하므로 별도 초기화 불필요.

```java
Path dataDir = Paths.get("data");
JsonSampleRepository sampleRepo = new JsonSampleRepository(dataDir.resolve("samples.json"));
JsonOrderRepository  orderRepo  = new JsonOrderRepository(dataDir.resolve("orders.json"));
```

### 1.3 시나리오 순서 및 출력 형식

각 단계 앞에 구분선과 단계 번호를 출력해 시각적으로 구분한다.

```
=== [1] Sample 3개 저장 ===
=== [2] findAll → 전체 시료 목록 ===
  [S001] GaN 웨이퍼 A | stock=50
  [S002] SiC 기판 B   | stock=0
  [S003] InP 기판 C   | stock=30
...
```

**Sample 시나리오 (단계 1~8)**

| 단계 | 동작 | 출력 내용 |
|------|------|-----------|
| 1 | `sampleRepo.save()` × 3 (S001·S002·S003) | `"[1] Sample 3개 저장 완료"` |
| 2 | `sampleRepo.findAll()` | 전체 시료 목록 (sampleId·name·stock) |
| 3 | `sampleRepo.findById("S002")` | S002 시료 상세 |
| 4 | S002 stock을 `999`로 변경 후 `sampleRepo.update()` | `"[4] S002 stock → 999 업데이트"` |
| 5 | `sampleRepo.findById("S002")` | 변경된 S002 확인 |
| 6 | `sampleRepo.deleteById("S003")` | `"[6] S003 삭제"` |
| 7 | `sampleRepo.findAll()` | S003 제거된 목록 (2개) |

**Order 시나리오 (단계 8~15)**

| 단계 | 동작 | 출력 내용 |
|------|------|-----------|
| 8 | `orderRepo.save()` × 3 (O001:RESERVED·O002:PRODUCING·O003:CONFIRMED) | `"[8] Order 3개 저장 완료"` |
| 9 | `orderRepo.findAll()` | 전체 주문 목록 (orderId·status) |
| 10 | `orderRepo.findByStatus(RESERVED)` | RESERVED 주문만 (O001) |
| 11 | O001 status를 CONFIRMED로 변경 후 `orderRepo.update()` | `"[11] O001 status → CONFIRMED 업데이트"` |
| 12 | `orderRepo.findByStatus(RESERVED)` | 빈 리스트 출력 |
| 13 | `orderRepo.deleteById("O002")` | `"[13] O002 삭제"` |
| 14 | `orderRepo.findAll()` | O002 제거된 목록 (2개) |
| 15 | 영속성 안내 메시지 | `"data/ 디렉토리의 JSON 파일로 데이터가 유지됩니다."` |

### 1.4 픽스처 데이터

**Sample 3개**

| ID | name | avgProductionTime | yield | stock |
|----|------|-------------------|-------|-------|
| S001 | GaN 웨이퍼 A | 120 | 0.85 | 50 |
| S002 | SiC 기판 B | 90 | 0.92 | 0 |
| S003 | InP 기판 C | 60 | 0.78 | 30 |

**Order 3개**

| ID | sampleId | customerName | quantity | status |
|----|----------|--------------|----------|--------|
| O001 | S001 | 서울대 나노연구소 | 10 | RESERVED |
| O002 | S002 | 카이스트 반도체랩 | 5 | PRODUCING |
| O003 | S001 | 연세대 소재공학과 | 3 | CONFIRMED |

### 1.5 재실행 시 중복 저장 방지

`./gradlew run`을 두 번 실행하면 `save()`에서 중복 ID `IllegalArgumentException`이 발생한다.
이를 방지하기 위해 시작 시 기존 JSON 파일을 삭제(`Files.deleteIfExists`)하거나
또는 단순히 `data/` 경로 파일을 지우는 안내를 출력한다.

**확정: 시작 시 data/ 파일 초기화** — 재실행 친화적이며 시나리오 결과를 항상 동일하게 유지한다.

```java
Files.deleteIfExists(dataDir.resolve("samples.json"));
Files.deleteIfExists(dataDir.resolve("orders.json"));
```

### 1.6 전체 코드

```java
package org.ssemi.persistence;

import org.ssemi.persistence.model.Order;
import org.ssemi.persistence.model.OrderStatus;
import org.ssemi.persistence.model.Sample;
import org.ssemi.persistence.repository.JsonOrderRepository;
import org.ssemi.persistence.repository.JsonSampleRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        Path dataDir = Paths.get("data");
        Files.deleteIfExists(dataDir.resolve("samples.json"));
        Files.deleteIfExists(dataDir.resolve("orders.json"));

        JsonSampleRepository sampleRepo = new JsonSampleRepository(dataDir.resolve("samples.json"));
        JsonOrderRepository  orderRepo  = new JsonOrderRepository(dataDir.resolve("orders.json"));

        // ── Sample 시나리오 ──────────────────────────────
        section("[1] Sample 3개 저장");
        sampleRepo.save(new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 50));
        sampleRepo.save(new Sample("S002", "SiC 기판 B",   90, 0.92,  0));
        sampleRepo.save(new Sample("S003", "InP 기판 C",   60, 0.78, 30));
        System.out.println("저장 완료");

        section("[2] findAll → 전체 시료 목록");
        printSamples(sampleRepo.findAll());

        section("[3] findById(S002)");
        sampleRepo.findById("S002").ifPresent(s -> System.out.println(formatSample(s)));

        section("[4] S002 stock → 999 업데이트");
        Sample s2 = sampleRepo.findById("S002").orElseThrow();
        s2.setStock(999);
        sampleRepo.update(s2);
        System.out.println("업데이트 완료");

        section("[5] findById(S002) — 변경 확인");
        sampleRepo.findById("S002").ifPresent(s -> System.out.println(formatSample(s)));

        section("[6] deleteById(S003)");
        sampleRepo.deleteById("S003");
        System.out.println("삭제 완료");

        section("[7] findAll → S003 제거 확인");
        printSamples(sampleRepo.findAll());

        // ── Order 시나리오 ───────────────────────────────
        section("[8] Order 3개 저장");
        orderRepo.save(new Order("O001", "S001", "서울대 나노연구소", 10, OrderStatus.RESERVED));
        orderRepo.save(new Order("O002", "S002", "카이스트 반도체랩",  5, OrderStatus.PRODUCING));
        orderRepo.save(new Order("O003", "S001", "연세대 소재공학과",  3, OrderStatus.CONFIRMED));
        System.out.println("저장 완료");

        section("[9] findAll → 전체 주문 목록");
        printOrders(orderRepo.findAll());

        section("[10] findByStatus(RESERVED)");
        printOrders(orderRepo.findByStatus(OrderStatus.RESERVED));

        section("[11] O001 status → CONFIRMED 업데이트");
        Order o1 = orderRepo.findById("O001").orElseThrow();
        o1.setStatus(OrderStatus.CONFIRMED);
        orderRepo.update(o1);
        System.out.println("업데이트 완료");

        section("[12] findByStatus(RESERVED) — 빈 리스트 확인");
        List<Order> reserved = orderRepo.findByStatus(OrderStatus.RESERVED);
        System.out.println(reserved.isEmpty() ? "(없음)" : reserved);

        section("[13] deleteById(O002)");
        orderRepo.deleteById("O002");
        System.out.println("삭제 완료");

        section("[14] findAll → O002 제거 확인");
        printOrders(orderRepo.findAll());

        section("[15] 영속성 안내");
        System.out.println("data/ 디렉토리의 JSON 파일로 데이터가 유지됩니다.");
        System.out.println("  → " + dataDir.resolve("samples.json").toAbsolutePath());
        System.out.println("  → " + dataDir.resolve("orders.json").toAbsolutePath());
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    private static String formatSample(Sample s) {
        return String.format("  [%s] %s | avgProdTime=%d | yield=%.2f | stock=%d",
                s.getSampleId(), s.getName(), s.getAvgProductionTime(), s.getYield(), s.getStock());
    }

    private static void printSamples(List<Sample> list) {
        if (list.isEmpty()) { System.out.println("  (없음)"); return; }
        list.forEach(s -> System.out.println(formatSample(s)));
    }

    private static String formatOrder(Order o) {
        return String.format("  [%s] sampleId=%s | customer=%s | qty=%d | status=%s",
                o.getOrderId(), o.getSampleId(), o.getCustomerName(), o.getQuantity(), o.getStatus());
    }

    private static void printOrders(List<Order> list) {
        if (list.isEmpty()) { System.out.println("  (없음)"); return; }
        list.forEach(o -> System.out.println(formatOrder(o)));
    }
}
```

---

## 2. 빌드 & 커버리지 검증

### 2.1 실행 명령

```bash
# CRUD 시나리오 실행
./gradlew run

# 전체 빌드 (컴파일 + 테스트 + JaCoCo 리포트)
./gradlew build

# 커버리지 임계값 검증 (90% 미만 시 빌드 실패)
./gradlew jacocoTestCoverageVerification
```

### 2.2 예상 콘솔 출력 (./gradlew run)

```
=== [1] Sample 3개 저장 ===
저장 완료

=== [2] findAll → 전체 시료 목록 ===
  [S001] GaN 웨이퍼 A | avgProdTime=120 | yield=0.85 | stock=50
  [S002] SiC 기판 B   | avgProdTime=90  | yield=0.92 | stock=0
  [S003] InP 기판 C   | avgProdTime=60  | yield=0.78 | stock=30

=== [3] findById(S002) ===
  [S002] SiC 기판 B | avgProdTime=90 | yield=0.92 | stock=0

=== [4] S002 stock → 999 업데이트 ===
업데이트 완료

=== [5] findById(S002) — 변경 확인 ===
  [S002] SiC 기판 B | avgProdTime=90 | yield=0.92 | stock=999

...

=== [15] 영속성 안내 ===
data/ 디렉토리의 JSON 파일로 데이터가 유지됩니다.
  → C:\reviewer\workspace\과제\DataPersistence\data\samples.json
  → C:\reviewer\workspace\과제\DataPersistence\data\orders.json
```

### 2.3 커버리지 리포트 위치

```
build/reports/tests/test/index.html           ← JUnit 테스트 결과
build/reports/jacoco/test/html/index.html     ← JaCoCo 커버리지
```

---

## 3. 완료 조건

- [ ] `Main.java` 생성
- [ ] `./gradlew run` — 15개 단계 전체 출력 확인
- [ ] `data/samples.json`·`data/orders.json` 파일 생성 확인
- [ ] `./gradlew build` — BUILD SUCCESSFUL
- [ ] JUnit 전체 테스트 63개 통과 (Phase 1·2·3 회귀 없음)
- [ ] `util`·`repository` 패키지 라인 커버리지 90% 이상
- [ ] PRD.md 섹션 10 완료 조건 전 항목 체크
