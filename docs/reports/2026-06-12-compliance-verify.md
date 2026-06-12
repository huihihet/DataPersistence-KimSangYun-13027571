# 컴플라이언스 검증 보고서

**일시**: 2026-06-12  
**검증 대상**: `DataPersistence/docs/design/phase1.md`  
**결과**: ❌ 위반 1건 (CRITICAL: 0, WARNING: 1)

---

## 발견된 위반

### [WARNING] Gson 역직렬화용 기본 생성자를 public으로 공개
- **위치**: `docs/design/phase1.md` — 2.3절 (Sample 생성자), 2.6절 (Sample 전체 코드), 3.3절 (Order 생성자), 3.6절 (Order 전체 코드)
- **위반 규칙**: 루트 `CLAUDE.md` 아키텍처 규칙 — Model 레이어는 "도메인 로직, 상태 관리, 유효성 검사" 담당. 인수 없는 `public` 기본 생성자는 필드가 모두 `null`/`0`인 무효 상태의 객체 생성을 애플리케이션 어디서나 허용하여, Model 레이어의 데이터 무결성 보호 책임을 약화시킨다.
- **현재 설계**:
  ```java
  // Sample.java
  public Sample() {}   // Gson 역직렬화용이지만 public 노출

  // Order.java
  public Order() {}    // Gson 역직렬화용이지만 public 노출
  ```
  설계 문서 2.3절·3.3절 생성자 목적란에 "Gson 역직렬화용 기본 생성자 (필드 초기화 없음)"라고 명시되어 있으나, 접근 제어자가 `public`으로 설계되어 외부에서 무효 객체를 직접 생성할 수 있다. Gson은 리플렉션으로 `private` 및 package-private 생성자에도 접근 가능하므로 직렬화 기능에 영향이 없다.
- **권장 수정**: 기본 생성자를 package-private(`/* package */`)으로 변경하여 외부 오용을 차단한다. 전체 인자 생성자만 `public`으로 유지한다.
  ```java
  // Sample.java
  /* package */ Sample() {}   // Gson 역직렬화 전용 — 외부 직접 사용 차단

  // Order.java
  /* package */ Order() {}    // Gson 역직렬화 전용 — 외부 직접 사용 차단
  ```

---

## 검증 결과 요약

- [A] 아키텍처 제약: ✅
  - `org.ssemi.persistence.model` 패키지 경로가 DataPersistence CLAUDE.md 패키지 구조와 일치
  - View·Controller import 없음
  - 비즈니스 로직(재고 계산, 상태 전이) 포함 없음
  - `System.out` 콘솔 I/O 없음
- [B] 코딩 컨벤션: ✅
  - 클래스명 PascalCase (`Sample`, `Order`, `OrderStatus`): 준수
  - 메서드명 camelCase (`getSampleId`, `getAvgProductionTime` 등): 준수
  - 필드명 camelCase (`sampleId`, `avgProductionTime`, `customerName` 등): 준수
  - 패키지명 전부 소문자 (`org.ssemi.persistence.model`): 준수
  - 코드 블록 내 WHAT 주석 없음: 준수
  - `toString` 불필요 주석 없음: 준수
  - `equals`/`hashCode`에 `Objects.equals`·`Objects.hash` 사용: Java 표준 관례 준수
- [C] 보안: ✅
  - 민감 정보 평문 저장 설계 없음
  - 입력 검증 로직 없음 (도메인 모델 전용으로 해당 없음)
  - SQL 인젝션·커맨드 인젝션 위험 없음
- [D] 불필요한 복잡성: ✅
  - 3개 클래스(`Sample`, `Order`, `OrderStatus`)로 최소 범위 유지
  - 불필요한 추상화 없음
  - 미래 확장 포인트 없음
