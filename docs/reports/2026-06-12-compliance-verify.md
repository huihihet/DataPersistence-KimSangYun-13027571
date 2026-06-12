# 컴플라이언스 검증 보고서

**일시**: 2026-06-12  
**검증 대상**: `DataPersistence/docs/PRD.md`, `DataPersistence/CLAUDE.md`  
**결과**: ❌ 위반 2건 (CRITICAL: 0, WARNING: 2)

---

## 발견된 위반

### [WARNING] CLAUDE.md Repository 인터페이스 규칙에 WHAT 주석 포함
- **위치**: `DataPersistence/CLAUDE.md` — "Repository 인터페이스 규칙" 섹션
- **위반 규칙**: 루트 `CLAUDE.md` 코딩 컨벤션 — "주석: WHY가 비자명한 경우에만 한 줄 이내로 작성"
- **현재 설계**:
  ```java
  void save(T entity);          // 신규 저장
  Optional<T> findById(ID id);  // 단건 조회
  List<T> findAll();            // 전체 조회
  void update(T entity);        // 수정 (id 기준 덮어쓰기)
  void deleteById(ID id);       // 삭제
  ```
  메서드명(`save`, `findById`, `findAll`, `update`, `deleteById`)만으로 의미가 자명함에도 WHAT을 설명하는 인라인 주석이 붙어 있다.
- **권장 수정**: 인라인 주석 전체 제거. 메서드명이 자명하므로 주석 불필요. 제네릭 타입 파라미터 제약 등 WHY가 비자명한 경우에만 주석을 허용한다.
  ```java
  void save(T entity);
  Optional<T> findById(ID id);
  List<T> findAll();
  void update(T entity);
  void deleteById(ID id);
  ```

---

### [WARNING] DataPersistence CLAUDE.md 패키지 구조에 OrderStatus.java 누락
- **위치**: `DataPersistence/CLAUDE.md` — "패키지 구조" 섹션 vs `DataPersistence/docs/PRD.md` — "7. 구현 패키지 구조" 섹션
- **위반 규칙**: 루트 `CLAUDE.md` 도메인 모델 — `OrderStatus`는 `Order`와 함께 정의되어야 하는 필수 enum. 설계 문서 내 패키지 구조 명세의 일관성 유지 의무.
- **현재 설계**:
  - `DataPersistence/CLAUDE.md` 패키지 구조: `model/Sample.java`, `model/Order.java` 만 명시, `OrderStatus.java` 누락
  - `DataPersistence/docs/PRD.md` 7절: `model/OrderStatus.java` 포함
  - `Order.java`가 `OrderStatus` 필드를 가지므로 `OrderStatus.java`는 필수 구성 요소임에도 CLAUDE.md에서 누락됨
- **권장 수정**: `DataPersistence/CLAUDE.md` 패키지 구조에 `OrderStatus.java` 추가
  ```
  ├── model/
  │   ├── Sample.java
  │   ├── Order.java
  │   └── OrderStatus.java    ← 추가
  ```

---

## 검증 결과 요약

- [A] 아키텍처 제약: ✅
  - PoC 스코프가 Repository 계층에만 집중 (Controller/View/비즈니스 로직 제외) — 준수
  - Repository 구현체에 비즈니스 로직 미포함으로 설계 — 준수
  - 레이어 분리 원칙 준수 (`JsonFileUtil`은 util 패키지, 엔티티는 model 패키지) — 준수
- [B] 코딩 컨벤션: ❌
  - 클래스명 PascalCase: 준수
  - 메서드명 camelCase: 준수
  - 인터페이스 `I` 접두사 금지: 준수
  - Repository 구현체 저장 방식 접두사: 준수
  - 주석 규칙(WHY만, WHAT 금지): ❌ CLAUDE.md Repository 인터페이스 규칙 코드 블록에 WHAT 주석 포함
- [C] 보안: ✅
  - 테스트 격리 규칙(`@TempDir`, `data/` 오염 금지) PRD.md 4절에 명시 — 준수
  - 파일 경로 전략 명시 — 준수
  - 도메인 모델에 민감 정보 없음 — 해당 없음
- [D] 불필요한 복잡성: ✅
  - PoC 범위를 영속성 계층으로 명확히 한정 — 준수
  - `findByStatus`는 OrderRepository 전용 메서드로 불필요한 공통 추상화 미적용 — 준수
  - 미래 확장 포인트 불필요 추가 없음 — 준수
