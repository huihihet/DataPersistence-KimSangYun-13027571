# 문서 정합성 검증 보고서

**일시**: 2026-06-12
**검증 문서**:
- `C:\reviewer\workspace\과제\CLAUDE.md` (루트 규칙)
- `C:\reviewer\workspace\과제\DataPersistence\CLAUDE.md` (서브 프로젝트 규칙)
- `C:\reviewer\workspace\과제\DataPersistence\docs\PRD.md` v1.0.0
- `C:\reviewer\workspace\과제\DataPersistence\docs\PLAN.md` v1.0.0

**결과**: 문제 4건 발견 (CRITICAL: 1, WARNING: 3, INFO: 1)

---

## 발견된 문제

### [CRITICAL] DataPersistence CLAUDE.md 패키지 구조에 OrderStatus.java 누락

- **위치**: `DataPersistence/CLAUDE.md` — 패키지 구조 섹션 (19~33행)
- **설명**: DataPersistence CLAUDE.md의 패키지 구조에는 `model/` 하위에 `Sample.java`와 `Order.java` 두 파일만 정의되어 있다. 반면 PRD.md 섹션 2.3에서 `OrderStatus` enum을 독립 파일로 정의하고, PRD.md 섹션 7 패키지 구조에도 `model/OrderStatus.java`를 포함시켰으며, PLAN.md Phase 1에서도 `model/OrderStatus.java`를 신규 생성 파일로 명시한다. CLAUDE.md와 PRD.md·PLAN.md 사이에 파일 목록이 불일치한다.
- **권장 조치**: DataPersistence CLAUDE.md의 패키지 구조에 `model/OrderStatus.java`를 추가하여 PRD.md 섹션 7 및 PLAN.md Phase 1과 일치시킨다.

---

### [WARNING-1] CLAUDE.md Repository 공통 시그니처와 PRD OrderRepository의 findByStatus 불일치

- **위치**: `DataPersistence/CLAUDE.md` — Repository 인터페이스 규칙 섹션 (64~72행) vs `docs/PRD.md` — 섹션 3.2
- **설명**: CLAUDE.md는 "모든 Repository가 공통으로 구현해야 할 CRUD 시그니처"로 5종(`save`, `findById`, `findAll`, `update`, `deleteById`)만 열거한다. PRD.md의 `OrderRepository`에는 이 5종 외에 `findByStatus(OrderStatus)` 메서드가 추가되어 있으며, PLAN.md Phase 4의 `OrderRepository` 인터페이스에도 동일하게 포함된다. CLAUDE.md 문구가 해당 5종이 전부인지(Closed), 최소 요건인지(Open) 명확하지 않아 확장 메서드 추가가 규칙 위반인지 모호하다.
- **권장 조치**: DataPersistence CLAUDE.md의 Repository 인터페이스 규칙 문구를 "공통 CRUD 최소 시그니처이며, 엔티티별 추가 조회 메서드는 허용한다"는 식으로 명확히 하거나, `findByStatus`를 CLAUDE.md 예시에 포함시켜 의도를 명시한다.

---

### [WARNING-2] PRD 섹션 8의 build.gradle 명세에 JaCoCo 설정 누락

- **위치**: `docs/PRD.md` — 섹션 8 "build.gradle 변경 사항"
- **설명**: PRD 섹션 8은 추가해야 할 `build.gradle` 내용으로 `application` 플러그인, Gson 의존성, JUnit 의존성만 나열한다. 그러나 PRD 섹션 6.6은 JaCoCo를 통한 커버리지 90% 목표를 요구하며, PLAN.md Phase 0의 최종 `build.gradle`에는 `id 'jacoco'` 플러그인, `jacocoTestReport` 블록, `jacocoTestCoverageVerification` 블록이 포함되어 있다. PRD 섹션 8이 실제로 필요한 전체 `build.gradle` 변경 사항을 완전히 반영하지 못한다.
- **권장 조치**: PRD 섹션 8에 JaCoCo 플러그인 및 관련 설정 블록(`jacocoTestReport`, `jacocoTestCoverageVerification`)을 추가하거나, 섹션 8 서두에 "아래는 핵심 의존성이며 JaCoCo 설정은 PLAN.md Phase 0을 참고한다"는 주석을 명시한다.

---

### [WARNING-3] null 인자 테스트 케이스가 PRD 섹션 6.5 정책 대비 불완전하게 적용됨

- **위치**: `docs/PLAN.md` — Phase 3 섹션 3-3, Phase 4 섹션 4-3
- **설명**: PRD 섹션 6.5는 다음 null 정책을 명시한다.
  - `save(null)`, `update(null)` → `NullPointerException` 또는 `IllegalArgumentException`
  - `findById(null)`, `deleteById(null)` → `IllegalArgumentException`
  - `findByStatus(null)` → `IllegalArgumentException`

  그러나 PLAN.md의 테스트 케이스에 아래 항목이 누락되어 있다.

  **Phase 3 JsonSampleRepositoryTest (10개)**: `save_null_throwsException`(#10)만 있고 `findById_null_throwsException`, `update_null_throwsException`, `deleteById_null_throwsException` 3개 테스트가 없다.

  **Phase 4 JsonOrderRepositoryTest (12개)**: `findByStatus_null_throwsException`(#6)만 있고 `save_null_throwsException`, `findById_null_throwsException`, `update_null_throwsException`, `deleteById_null_throwsException` 4개 테스트가 없다.

  null 테스트 케이스를 완전히 보완하면 총 테스트 수는 39개가 되어 Phase 6 완료 조건의 "32개 이상" 기준은 여전히 유효하지만, 문서 간 정합 차원에서 누락이 존재한다.

- **권장 조치**: PLAN.md Phase 3의 `JsonSampleRepositoryTest`에 `findById_null_throwsException`, `update_null_throwsException`, `deleteById_null_throwsException` 3개 테스트를 추가한다. PLAN.md Phase 4의 `JsonOrderRepositoryTest`에 `save_null_throwsException`, `findById_null_throwsException`, `update_null_throwsException`, `deleteById_null_throwsException` 4개 테스트를 추가한다.

---

### [INFO-1] 루트 CLAUDE.md(80%)와 DataPersistence CLAUDE.md(90%) 커버리지 목표 수치 상이

- **위치**: `C:\reviewer\workspace\과제\CLAUDE.md` — "테스트 전략" 섹션 vs `C:\reviewer\workspace\과제\DataPersistence\CLAUDE.md` — "기술 스택 (PoC 한정)" 섹션
- **설명**: 루트 CLAUDE.md는 `Model·Controller 핵심 로직 80% 이상`을 목표 커버리지로 명시한다. DataPersistence CLAUDE.md는 `Repository·Util 핵심 로직 90% 이상`으로 상향 설정하며 PRD.md 섹션 6.6 및 PLAN.md와 일치한다. 서브 프로젝트가 루트 규칙을 상속하면서 수치를 상향 조정하는 것은 설계 의도상 허용되나, 루트 CLAUDE.md는 이 차이를 명시하지 않는다.
- **권장 조치**: 루트 CLAUDE.md에 "각 서브 프로젝트는 개별 CLAUDE.md에서 더 높은 커버리지 목표를 설정할 수 있다"는 주석을 추가하거나, DataPersistence CLAUDE.md에 "루트 80% 기준에서 PoC 특성상 90%로 상향"임을 명시하여 의도적 차이임을 문서화한다.

---

## 통과 항목

- **[A] 교차 참조 일관성**: 문제 1건 (CRITICAL — DataPersistence CLAUDE.md의 OrderStatus.java 누락)
- **[B] 기술 스택 일관성**: 이상 없음 — Gson 2.11.0, JUnit Jupiter 6.x(`junit-bom:6.0.0`), Java 17, Gradle 8.x가 CLAUDE.md·PRD.md·PLAN.md에서 일관되게 사용됨. CLAUDE.md에 없는 신규 외부 의존성 없음.
- **[C] 설계 제약 반영**: 이상 없음 — Controller/View 레이어 제외, 비즈니스 로직 미포함 제약이 PLAN.md 전 Phase에서 준수됨.
- **[D] 완료 기준 명시**: 이상 없음 — Phase 0~6 모두 완료 조건(`- [ ]` 체크리스트) 명시됨. PRD.md 섹션 10도 완료 조건 목록 보유.
- **[E] 인터페이스 시그니처**: 이상 없음 — PRD.md 섹션 3.1(SampleRepository), 3.2(OrderRepository), 3.3(JsonFileUtil) 메서드 시그니처가 PLAN.md Phase 3·4에서 동일하게 구현 명세에 반영됨.
- **[F] 테스트 픽스처 전략**: 이상 없음 — PRD.md 섹션 6.4의 `@TempDir` + `@BeforeEach` 초기화 전략이 PLAN.md Phase 2·3·4에서 동일하게 적용됨.
- **[G] 패키지 루트**: 이상 없음 — `org.ssemi.persistence` 패키지 루트가 DataPersistence CLAUDE.md, PRD.md 섹션 7, PLAN.md 전 Phase에서 일치함.
- **[H] 영속성 파일 경로**: 이상 없음 — `data/samples.json`, `data/orders.json` 경로가 PRD.md 섹션 4, 섹션 5, PLAN.md Phase 5 전반에서 일관됨.
