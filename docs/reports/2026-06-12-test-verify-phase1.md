# 테스트 전략 검증 보고서

**일시**: 2026-06-12
**검증 대상**: `docs/design/phase1.md` (Phase 1 — 도메인 모델 설계)
**참조 문서**: `docs/PLAN.md`, `docs/PRD.md`, `DataPersistence/CLAUDE.md`, `과제/CLAUDE.md`
**결과**: 미흡 4건 (CRITICAL: 2, WARNING: 2)

---

## 발견된 문제

### [CRITICAL-1] model 패키지 전체가 공식 테스트 계획 범위 밖에 있음

- **대상 기능**: `OrderStatus`, `Sample`, `Order` (Phase 1 전체)
- **문제**: phase1.md 완료 조건(섹션 5)은 `./gradlew compileJava` 통과와
  수동 setter/getter 확인 두 항목만 포함한다. PLAN.md Phase 1 완료 조건도
  동일하게 "컴파일 오류 없음"과 "수동 또는 테스트"라는 모호한 표현에 그친다.
  PLAN.md Phase 2~4 및 PRD.md 섹션 6은 `util`·`repository` 패키지에만 테스트
  케이스를 정의하며, `model` 패키지를 독립 테스트 대상으로 다루지 않는다.
  PLAN.md Phase 6 완료 조건과 PRD.md 섹션 6.6 커버리지 목표 모두 `util`·`repository`
  패키지만 대상이며, `model` 패키지는 JaCoCo 목표에서도 제외되어 있다.
  Phase 3·4 테스트가 `Sample`·`Order`를 픽스처로 사용하여 일부 코드 경로를 간접
  실행하지만, `equals`/`hashCode`/`toString`의 분기 전체가 체계적으로 커버된다는
  보장이 없으며 이러한 의도도 어디에도 명시되지 않았다.
- **권장 조치**:
  - 옵션 A: Phase 1 완료 조건에 model 패키지 단위 테스트 파일
    (`src/test/java/org/ssemi/persistence/model/SampleTest.java`,
    `OrderTest.java`)을 추가하고 테스트 케이스를 명시한다.
  - 옵션 B: phase1.md 섹션 6(다음 Phase 연계)과 PLAN.md에 "model 패키지 테스트는
    Phase 3·4 Repository 테스트에서 간접 커버한다"는 의도를 명문화하고,
    Phase 6 완료 조건에 `model` 패키지 커버리지 항목을 추가한다.
- **권장 테스트 케이스 예시 (옵션 A 선택 시)**:
  ```
  // SampleTest
  fullArgsConstructor_fieldsSetCorrectly
  equals_sameId_returnsTrue
  equals_differentId_returnsFalse
  equals_null_returnsFalse
  equals_differentType_returnsFalse
  hashCode_sameId_sameHash
  hashCode_differentId_differentHash
  toString_containsSampleIdAndNameAndStock

  // OrderTest (동일 패턴 + 상태 불변성)
  equals_sameOrderId_returnsTrue
  equals_statusDiffers_stillEqual   // 상태 달라도 동일 주문임을 확인
  equals_null_returnsFalse
  ```

---

### [CRITICAL-2] equals의 null ID 엣지케이스 미커버

- **대상 기능**: `Sample.equals`, `Order.equals`
- **문제**: 두 클래스 모두 기본 생성자(`Sample()`, `Order()`)를 제공하며 `sampleId`/
  `orderId`를 초기화하지 않는다(기본값 `null`). 이 상태에서 두 인스턴스에 대해
  `a.equals(b)`를 호출하면 `Objects.equals(null, null)`이 `true`를 반환하여
  서로 다른 객체가 동일 취급된다.

  phase1.md 섹션 2.5는 "sampleId만 기준으로 한다"고 설명하지만 null sampleId
  시나리오를 명시적으로 다루지 않는다. 기본 생성자는 Gson 역직렬화용으로 제공되지만,
  JSON 파일에 `"sampleId"` 키가 누락된 레코드를 역직렬화하면 sampleId가 null인
  `Sample` 객체가 생성된다. 이 경우 Repository의 `findAll()`이 동일 객체를 복수
  반환하거나 `save`의 중복 ID 검사가 오동작할 수 있다.

  Phase 3 `JsonSampleRepositoryTest` 어디에도 이 케이스를 커버하는 테스트가 없다.
- **권장 조치**:
  1. phase1.md 섹션 2.5/3.5에 "sampleId/orderId가 null인 경우의 equals 동작"을
     설계 결정으로 명시한다.
     - 옵션 A: null ID를 허용하고 `null == null`을 동일로 간주 (현재 구현 동작, 위험)
     - 옵션 B: null ID를 가진 객체는 항상 `equals = false` 처리
       (`if (sampleId == null) return false;` 추가)
  2. 선택한 동작에 맞는 테스트 케이스를 명시한다.
- **권장 테스트 케이스 예시**:
  ```
  equals_bothNullId_definedBehavior   // 옵션 A면 true, 옵션 B면 false
  equals_oneNullId_returnsFalse
  hashCode_nullId_doesNotThrow        // NPE 발생 여부 확인 (Objects.hash(null) = 0, 정상)
  ```

---

### [WARNING-1] 필드 제약(음수·범위 초과) 검증 계획 미명시

- **대상 기능**: `Sample` setter 4종, `Order.setQuantity`
- **문제**: phase1.md 섹션 2.2는 `avgProductionTime >= 0`, `0.0 <= yield <= 1.0`,
  `stock >= 0`, `quantity >= 1` 제약을 명시한다. 그러나 섹션 2.6 전체 코드를 보면
  setter에 유효성 검사 로직이 없어 음수 재고·수량, 1.0 초과 수율이 허용된다.
  이것이 의도적인 결정(PoC에서 검증 생략)인지 누락인지 설계 문서에서 불분명하며,
  테스트 계획에도 관련 언급이 없다.
  이 상태로 SSemi에 이식하면 잘못된 도메인 값이 JSON에 저장되는 버그가 발생할 수 있다.
- **권장 조치**:
  - "PoC에서는 엔티티 레벨 유효성 검사를 생략하고 Repository 또는 상위 레이어에서
    처리한다"는 결정을 phase1.md에 명시적으로 기술한다.
  - 또는 setter에 검증 로직을 추가하고 해당 케이스를 테스트 계획에 포함한다.
- **권장 테스트 케이스 예시 (검증 로직 추가 시)**:
  ```
  setStock_negative_throwsIllegalArgumentException
  setYield_greaterThanOne_throwsIllegalArgumentException
  setYield_negative_throwsIllegalArgumentException
  setQuantity_zero_throwsIllegalArgumentException
  setAvgProductionTime_negative_throwsIllegalArgumentException
  ```

---

### [WARNING-2] Phase 1 생성자 시그니처 변경 시 하위 테스트 파급 범위 미명시

- **대상 기능**: Phase 3 `JsonSampleRepositoryTest`, Phase 4 `JsonOrderRepositoryTest`
- **문제**: PLAN.md Phase 3·4의 테스트 케이스 전체는 `Sample`·`Order` 전체 인자
  생성자를 픽스처로 사용한다. Phase 1의 생성자 시그니처 또는 필드가 변경되면
  Phase 3·4 테스트 파일 전체가 컴파일 오류를 일으킨다. 이 의존 관계가 문서 어디에도
  명시되지 않아, Phase 1 변경 시 영향 범위를 파악하기 어렵다.
  특히 phase1.md 섹션 6(다음 Phase 연계)은 "Phase 2가 Sample·Order를 직렬화 대상으로
  사용한다"만 언급하고, Phase 3·4 테스트 픽스처 의존성은 기술하지 않는다.
- **권장 조치**:
  - PLAN.md 리스크 섹션 또는 Phase 1 완료 조건에 "Phase 1 생성자·필드 변경 시
    Phase 3·4 테스트 픽스처 동반 수정 필요"를 추가한다.
  - phase1.md 섹션 6에 "Phase 3·4 테스트는 전체 인자 생성자에 의존한다"는 내용을
    추가한다.

---

## 검증 결과 요약

| 항목 | 결과 | 비고 |
|------|------|------|
| [A] 테스트 계획 존재 | 미흡 | model 패키지 테스트 케이스 전무, 수동 확인만 명시 |
| [B] 엣지케이스 식별 | 미흡 | null ID equals 동작 미정의, 필드 범위 초과 케이스 미계획 |
| [C] 기존 테스트 충돌 | 통과 | 현재 테스트 파일 없음; Phase 간 의존성은 WARNING-2로 기록 |
| [D] 테스트 구조 적절성 | 미흡 | model 패키지 커버리지 목표 미설정, 간접 커버 의도 미명시 |
