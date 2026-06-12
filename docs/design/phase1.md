# Phase 1 — 도메인 모델 설계

**버전**: 1.0.0
**작성일**: 2026-06-12
**기반 문서**: `docs/PLAN.md` Phase 1, `docs/PRD.md` 섹션 2
**상태**: 초안

---

## 목표

JSON 직렬화·역직렬화 대상 엔티티 클래스를 작성한다.
이 클래스들은 Phase 2 이후 모든 레이어가 의존하는 최하위 기반이다.

---

## 생성 파일

```
src/main/java/org/ssemi/persistence/model/
├── OrderStatus.java
├── Sample.java
└── Order.java
```

---

## 1. OrderStatus

### 1.1 파일 정보

| 항목 | 값 |
|------|-----|
| 파일 경로 | `src/main/java/org/ssemi/persistence/model/OrderStatus.java` |
| 종류 | `enum` |
| 패키지 | `org.ssemi.persistence.model` |

### 1.2 상수 목록

| 상수 | 의미 |
|------|------|
| `RESERVED` | 주문 접수 |
| `PRODUCING` | 승인 완료, 재고 부족으로 생산 중 |
| `CONFIRMED` | 승인 완료, 출고 대기 |
| `RELEASE` | 출고 완료 |
| `REJECTED` | 주문 거절 |

### 1.3 설계 결정

- 추가 필드·메서드 없음 — 이 PoC에서 `OrderStatus`는 상태값 식별자 역할만 한다.
- Gson은 enum을 기본적으로 `.name()` 문자열로 직렬화하므로 별도 어댑터 불필요.

### 1.4 전체 코드

```java
package org.ssemi.persistence.model;

public enum OrderStatus {
    RESERVED, PRODUCING, CONFIRMED, RELEASE, REJECTED
}
```

---

## 2. Sample

### 2.1 파일 정보

| 항목 | 값 |
|------|-----|
| 파일 경로 | `src/main/java/org/ssemi/persistence/model/Sample.java` |
| 종류 | `class` |
| 패키지 | `org.ssemi.persistence.model` |

### 2.2 필드 명세

| 필드명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `sampleId` | `String` | not null, 고유 | 고유 식별자 (e.g., `"S001"`) |
| `name` | `String` | not null | 시료 이름 |
| `avgProductionTime` | `int` | ≥ 0 | 평균 생산 시간(분) |
| `yield` | `double` | 0.0 ≤ yield ≤ 1.0 | 수율 |
| `stock` | `int` | ≥ 0 | 현재 재고 수량 |

### 2.3 생성자

| 생성자 | 접근 제어자 | 목적 |
|--------|------------|------|
| `Sample()` | package-private | Gson 역직렬화용 기본 생성자. 리플렉션 접근 가능하므로 `public` 불필요. 외부 오용 차단. |
| `Sample(String sampleId, String name, int avgProductionTime, double yield, int stock)` | `public` | 애플리케이션 코드에서 사용하는 전체 인자 생성자 |

### 2.4 메서드 목록

| 메서드 시그니처 | 설명 |
|----------------|------|
| `String getSampleId()` | |
| `void setSampleId(String sampleId)` | |
| `String getName()` | |
| `void setName(String name)` | |
| `int getAvgProductionTime()` | |
| `void setAvgProductionTime(int avgProductionTime)` | |
| `double getYield()` | |
| `void setYield(double yield)` | |
| `int getStock()` | |
| `void setStock(int stock)` | |
| `boolean equals(Object o)` | `sampleId` 동등성 비교 |
| `int hashCode()` | `sampleId` 기반 해시 |
| `String toString()` | 디버그용 (`sampleId`, `name`, `stock` 포함) |

### 2.5 `equals` / `hashCode` 설계

`sampleId`만 기준으로 한다. 재고·수율 등 가변 속성이 같아도 동일 시료로 간주하기 위함이다.

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Sample sample = (Sample) o;
    return Objects.equals(sampleId, sample.sampleId);
}

@Override
public int hashCode() {
    return Objects.hash(sampleId);
}
```

### 2.6 전체 코드

```java
package org.ssemi.persistence.model;

import java.util.Objects;

public class Sample {

    private String sampleId;
    private String name;
    private int avgProductionTime;
    private double yield;
    private int stock;

    Sample() {}

    public Sample(String sampleId, String name, int avgProductionTime, double yield, int stock) {
        this.sampleId = sampleId;
        this.name = name;
        this.avgProductionTime = avgProductionTime;
        this.yield = yield;
        this.stock = stock;
    }

    public String getSampleId() { return sampleId; }
    public void setSampleId(String sampleId) { this.sampleId = sampleId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAvgProductionTime() { return avgProductionTime; }
    public void setAvgProductionTime(int avgProductionTime) { this.avgProductionTime = avgProductionTime; }

    public double getYield() { return yield; }
    public void setYield(double yield) { this.yield = yield; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sample sample = (Sample) o;
        return Objects.equals(sampleId, sample.sampleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sampleId);
    }

    @Override
    public String toString() {
        return "Sample{sampleId='" + sampleId + "', name='" + name + "', stock=" + stock + "}";
    }
}
```

---

## 3. Order

### 3.1 파일 정보

| 항목 | 값 |
|------|-----|
| 파일 경로 | `src/main/java/org/ssemi/persistence/model/Order.java` |
| 종류 | `class` |
| 패키지 | `org.ssemi.persistence.model` |

### 3.2 필드 명세

| 필드명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| `orderId` | `String` | not null, 고유 | 고유 식별자 (e.g., `"O001"`) |
| `sampleId` | `String` | not null | 주문 시료 ID (Sample과 논리적 연관) |
| `customerName` | `String` | not null | 고객명 |
| `quantity` | `int` | ≥ 1 | 주문 수량 |
| `status` | `OrderStatus` | not null | 주문 상태 |

### 3.3 생성자

| 생성자 | 접근 제어자 | 목적 |
|--------|------------|------|
| `Order()` | package-private | Gson 역직렬화용 기본 생성자. 리플렉션 접근 가능하므로 `public` 불필요. 외부 오용 차단. |
| `Order(String orderId, String sampleId, String customerName, int quantity, OrderStatus status)` | `public` | 애플리케이션 코드에서 사용하는 전체 인자 생성자 |

### 3.4 메서드 목록

| 메서드 시그니처 | 설명 |
|----------------|------|
| `String getOrderId()` | |
| `void setOrderId(String orderId)` | |
| `String getSampleId()` | |
| `void setSampleId(String sampleId)` | |
| `String getCustomerName()` | |
| `void setCustomerName(String customerName)` | |
| `int getQuantity()` | |
| `void setQuantity(int quantity)` | |
| `OrderStatus getStatus()` | |
| `void setStatus(OrderStatus status)` | |
| `boolean equals(Object o)` | `orderId` 동등성 비교 |
| `int hashCode()` | `orderId` 기반 해시 |
| `String toString()` | 디버그용 (`orderId`, `sampleId`, `status` 포함) |

### 3.5 `equals` / `hashCode` 설계

`orderId`만 기준으로 한다. 상태가 변경되어도 동일 주문임을 유지해야 하기 때문이다.

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Order order = (Order) o;
    return Objects.equals(orderId, order.orderId);
}

@Override
public int hashCode() {
    return Objects.hash(orderId);
}
```

### 3.6 전체 코드

```java
package org.ssemi.persistence.model;

import java.util.Objects;

public class Order {

    private String orderId;
    private String sampleId;
    private String customerName;
    private int quantity;
    private OrderStatus status;

    Order() {}

    public Order(String orderId, String sampleId, String customerName, int quantity, OrderStatus status) {
        this.orderId = orderId;
        this.sampleId = sampleId;
        this.customerName = customerName;
        this.quantity = quantity;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getSampleId() { return sampleId; }
    public void setSampleId(String sampleId) { this.sampleId = sampleId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return "Order{orderId='" + orderId + "', sampleId='" + sampleId + "', status=" + status + "}";
    }
}
```

---

## 4. 필드 제약 및 setter 유효성 정책

이 PoC는 영속성 계층 검증이 목적이므로 **setter에서 유효성 검사를 수행하지 않는다.**
입력값 검증은 SSemi 본 프로젝트의 Controller 레이어 책임이다.

| 필드 | 논리적 제약 | setter 검사 여부 | 이유 |
|------|------------|-----------------|------|
| `sampleId`, `orderId` | not null, 고유 | 검사 안 함 | 중복 ID 검사는 Repository 책임 |
| `avgProductionTime`, `stock` | ≥ 0 | 검사 안 함 | Controller에서 파싱·검증 |
| `yield` | 0.0 ≤ yield ≤ 1.0 | 검사 안 함 | Controller에서 파싱·검증 |
| `quantity` | ≥ 1 | 검사 안 함 | Controller에서 파싱·검증 |
| `status` | not null | 검사 안 함 | Repository에서 null 인자 정책 적용 |

---

## 5. 단위 테스트 계획

Phase 1 도메인 모델은 단순 POJO이므로 별도 테스트 파일을 작성한다.
equals/hashCode의 정확성은 Phase 3·4 Repository 테스트에서 간접 검증되지 않는
엣지케이스(null ID)가 존재하므로 이 Phase에서 직접 검증한다.

### 생성 파일

```
src/test/java/org/ssemi/persistence/model/
├── SampleTest.java
└── OrderTest.java
```

### 5.1 SampleTest.java

| # | 테스트명 | 검증 내용 |
|---|---------|-----------|
| 1 | `allArgsConstructor_fieldsSetCorrectly` | 전체 인자 생성자로 생성 후 모든 getter 반환값 확인 |
| 2 | `setters_updateFields` | setter 호출 후 getter로 변경 확인 |
| 3 | `equals_sameSampleId_returnsTrue` | 동일 sampleId → `equals` true |
| 4 | `equals_differentSampleId_returnsFalse` | 다른 sampleId → `equals` false |
| 5 | `equals_nullSampleId_bothNull_returnsTrue` | 두 인스턴스 모두 sampleId=null → `equals` true |
| 6 | `equals_nullSampleId_oneNull_returnsFalse` | 한쪽만 sampleId=null → `equals` false |
| 7 | `equals_null_returnsFalse` | `sample.equals(null)` → false |
| 8 | `equals_differentType_returnsFalse` | 다른 타입 객체 → false |
| 9 | `hashCode_sameSampleId_sameHash` | 동일 sampleId → 동일 hashCode |
| 10 | `toString_containsSampleIdAndName` | toString에 sampleId·name 포함 확인 |

### 5.2 OrderTest.java

| # | 테스트명 | 검증 내용 |
|---|---------|-----------|
| 1 | `allArgsConstructor_fieldsSetCorrectly` | 전체 인자 생성자로 생성 후 모든 getter 반환값 확인 |
| 2 | `setters_updateFields` | setter 호출 후 getter로 변경 확인 |
| 3 | `equals_sameOrderId_returnsTrue` | 동일 orderId → `equals` true |
| 4 | `equals_differentOrderId_returnsFalse` | 다른 orderId → `equals` false |
| 5 | `equals_nullOrderId_bothNull_returnsTrue` | 두 인스턴스 모두 orderId=null → `equals` true |
| 6 | `equals_nullOrderId_oneNull_returnsFalse` | 한쪽만 orderId=null → `equals` false |
| 7 | `equals_null_returnsFalse` | `order.equals(null)` → false |
| 8 | `equals_differentType_returnsFalse` | 다른 타입 객체 → false |
| 9 | `hashCode_sameOrderId_sameHash` | 동일 orderId → 동일 hashCode |
| 10 | `toString_containsOrderIdAndStatus` | toString에 orderId·status 포함 확인 |

### 5.3 Gson 간접 커버 관계

아래 케이스는 Phase 1 테스트에서 다루지 않으며, 이후 Phase에서 간접 검증한다.

| 케이스 | 커버 Phase | 테스트 |
|--------|-----------|--------|
| 기본 생성자 역직렬화 | Phase 2 | `JsonFileUtilTest` #2 |
| enum 직렬화·역직렬화 | Phase 4 | `JsonOrderRepositoryTest` #4 |
| `double yield` 정밀도 | Phase 2 | `JsonFileUtilTest` #7 |

---

## 7. Gson 직렬화 호환성 검토

| 항목 | 확인 결과 |
|------|-----------|
| 기본 생성자 존재 여부 | `Sample()`, `Order()` 모두 있음 — Gson 역직렬화 가능 |
| `OrderStatus` enum 직렬화 | Gson 기본 동작: `.name()` 문자열로 직렬화·역직렬화 — 별도 어댑터 불필요 |
| `double yield` 정밀도 | Gson `double` 기본 처리로 충분; `0.0`·`1.0` 경계값 손실 없음 |
| `int` 필드 기본값 | 역직렬화 시 JSON에 없는 `int` 필드는 `0`으로 초기화됨 — 허용 범위 내 |
| 필드 접근 방식 | Gson은 `private` 필드를 리플렉션으로 직접 접근 — getter/setter 없이도 동작하나, Phase 3~4에서 Repository가 setter를 호출하므로 유지 |

---

## 8. 완료 조건

- [ ] `OrderStatus.java` 생성 — 5개 상수 확인
- [ ] `Sample.java` 생성 — package-private 기본 생성자·전체 인자 생성자·getter/setter/equals/hashCode/toString 확인
- [ ] `Order.java` 생성 — package-private 기본 생성자·전체 인자 생성자·getter/setter/equals/hashCode/toString 확인
- [ ] `./gradlew compileJava` — 오류 없음
- [ ] `SampleTest` 10개 테스트 모두 통과
- [ ] `OrderTest` 10개 테스트 모두 통과
- [ ] `./gradlew test` — `model` 패키지 테스트 20개 전체 통과

---

## 9. 다음 Phase 연계

Phase 2(`JsonFileUtil`)는 이 Phase에서 완성된 `Sample`·`Order` 클래스를 직렬화 대상으로 사용한다.
Phase 2 진입 전 이 Phase의 완료 조건이 모두 충족되어야 한다.
