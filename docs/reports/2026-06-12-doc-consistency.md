# 문서 정합성 검증 보고서

**일시**: 2026-06-12
**검증 문서**:
- `C:\reviewer\workspace\과제\CLAUDE.md` (루트 규칙)
- `C:\reviewer\workspace\과제\DataPersistence\CLAUDE.md` (서브 프로젝트 규칙)
- `C:\reviewer\workspace\과제\DataPersistence\docs\PRD.md` v1.0.0
- `C:\reviewer\workspace\과제\DataPersistence\docs\PLAN.md` v1.0.0
- `C:\reviewer\workspace\과제\DataPersistence\docs\design\phase1.md`
- `C:\reviewer\workspace\과제\DataPersistence\docs\design\phase2.md` (신규)

**결과**: 문제 5건 발견 (CRITICAL: 1, WARNING: 3, INFO: 2)

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

  > **참고**: PLAN.md는 이후 갱신(Phase 3: 13개, Phase 4: 16개)을 통해 위 누락 항목이 이미 반영되어 있음이 확인되었다. PLAN.md와 phase2.md 기준으로는 이상 없음.

---

### [INFO-1] 루트 CLAUDE.md(80%)와 DataPersistence CLAUDE.md(90%) 커버리지 목표 수치 상이

- **위치**: `C:\reviewer\workspace\과제\CLAUDE.md` — "테스트 전략" 섹션 vs `C:\reviewer\workspace\과제\DataPersistence\CLAUDE.md` — "기술 스택 (PoC 한정)" 섹션
- **설명**: 루트 CLAUDE.md는 `Model·Controller 핵심 로직 80% 이상`을 목표 커버리지로 명시한다. DataPersistence CLAUDE.md는 `Repository·Util 핵심 로직 90% 이상`으로 상향 설정하며 PRD.md 섹션 6.6 및 PLAN.md와 일치한다. 서브 프로젝트가 루트 규칙을 상속하면서 수치를 상향 조정하는 것은 설계 의도상 허용되나, 루트 CLAUDE.md는 이 차이를 명시하지 않는다.
- **권장 조치**: 루트 CLAUDE.md에 "각 서브 프로젝트는 개별 CLAUDE.md에서 더 높은 커버리지 목표를 설정할 수 있다"는 주석을 추가하거나, DataPersistence CLAUDE.md에 "루트 80% 기준에서 PoC 특성상 90%로 상향"임을 명시하여 의도적 차이임을 문서화한다.

---

### [INFO-2] phase2.md의 writeAll 동작 명세 단계 구조 불일치

- **위치**: `docs/design/phase2.md` — 섹션 1.4 `writeAll` 단계 표 vs `docs/PLAN.md` — Phase 2 `writeAll` 동작 명세
- **설명**: PLAN.md Phase 2는 `writeAll` 동작 명세를 5단계로 기술한다.

  | 단계 | PLAN.md |
  |------|---------|
  | 1 | `filePath == null` → `IllegalArgumentException` |
  | 2 | `items == null` → `IllegalArgumentException` |
  | 3 | 부모 디렉토리 없으면 `Files.createDirectories(filePath.getParent())` |
  | 4 | 파일 쓰기: `Files.writeString(filePath, gson.toJson(items), StandardCharsets.UTF_8)` |
  | 5 | 전체 목록 덮어쓰기 (append 금지) |

  phase2.md 섹션 1.4의 `writeAll` 단계 표는 4단계만 포함하며, 5번째 단계("전체 목록 덮어쓰기, append 금지")가 표 밖에 별도 문장("전체 목록 덮어쓰기 방식이다. `StandardOpenOption.APPEND` 사용 금지.")으로 분리되어 있다.

  의미상 내용은 동일하나 PLAN.md가 명시한 5단계 구조와 phase2.md의 4단계 표 구조가 형식적으로 불일치한다.

- **권장 조치**: phase2.md의 `writeAll` 단계 표에 5번째 행을 추가하여 PLAN.md 구조와 형식적으로도 일치시킨다.

  추가할 행:
  | 5 | 항상 | 전체 목록 덮어쓰기 방식 (`StandardOpenOption.APPEND` 사용 금지) |

---

## 통과 항목

| 검증 항목 | 결과 | 비고 |
|----------|------|------|
| [A-1] PLAN.md Phase 목록과 실제 파일 일치 | 이상 없음 | phase1.md·phase2.md 모두 존재, PLAN.md 목록과 일치 |
| [A-2] phase2.md 내 참조 문서 경로 정확성 | 이상 없음 | 헤더 기반 문서 `docs/PLAN.md`, `docs/PRD.md` 참조 정확 |
| [B-1] 기술 스택 일관성 — 언어 | 이상 없음 | 전 문서 Java 17+ 일치 |
| [B-2] 기술 스택 일관성 — JSON 라이브러리 | 이상 없음 | 전 문서 Gson 2.11+ 일치 |
| [B-3] 기술 스택 일관성 — 테스트 프레임워크 | 이상 없음 | DataPersistence CLAUDE.md·PRD.md·phase2.md 모두 JUnit Jupiter 6.x 일치 |
| [B-4] 기술 스택 일관성 — 커버리지 도구 | 이상 없음 | 전 문서 JaCoCo 90% 이상 일치 |
| [B-5] 새 외부 의존성 추가 여부 | 이상 없음 | phase2.md에서 CLAUDE.md에 없는 신규 의존성 없음 |
| [C] 설계 제약 반영 — 수정 금지 위반 | 이상 없음 | phase2.md는 신규 파일 생성만 기술, 기존 파일 수정 없음 |
| [D] 완료 조건 누락 | 이상 없음 | phase2.md 섹션 5에 완료 조건 4항목 명시, PLAN.md Phase 2 완료 조건 포함 |
| [E-1] 패키지명 일치 | 이상 없음 | `org.ssemi.persistence.util` 전 문서 일치 |
| [E-2] readAll 메서드 시그니처 | 이상 없음 | PRD.md 3.3, PLAN.md Phase 2, phase2.md 1.4 일치 |
| [E-3] writeAll 메서드 시그니처 | 이상 없음 | PRD.md 3.3, PLAN.md Phase 2, phase2.md 1.4 일치 |
| [E-4] readAll 동작 5단계 내용 | 이상 없음 | PLAN.md와 phase2.md 내용 동일 (단계 수·순서 일치) |
| [E-5] writeAll 동작 내용 — 의미적 일치 | 이상 없음 | append 금지 포함, 모든 동작 내용 일치 (형식 불일치는 INFO-2) |
| [E-6] 테스트 케이스 10개 — PRD.md 6.1 대응 | 이상 없음 | PRD.md 7개 + null 3개 = 10개, phase2.md 테스트명·검증 내용 일치 |
| [E-7] 픽스처 전략 @TempDir | 이상 없음 | PRD.md 6.4, PLAN.md, phase2.md 모두 @TempDir 사용 일치 |
| [E-8] 선행 Phase 명시 | 이상 없음 | phase2.md 헤더 및 목표 섹션에 Phase 1 완료 필요 명시 |
| [E-9] 내부 모순 — 문서 간 상충 결정 | 이상 없음 | INFO-2 단계 수 차이 외 상충 결정 없음 |
| [F] 테스트 픽스처 전략 | 이상 없음 | PRD.md 섹션 6.4의 `@TempDir` 전략이 phase2.md에서 동일하게 적용됨 |
| [G] 패키지 루트 | 이상 없음 | `org.ssemi.persistence` 패키지 루트가 전 문서 일치 |
| [H] 영속성 파일 경로 | 이상 없음 | `data/samples.json`, `data/orders.json` 경로 전 문서 일관 |
