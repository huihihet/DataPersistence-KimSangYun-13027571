# 문서 정합성 검증 보고서 — Phase 3

**일시**: 2026-06-12
**검증 문서**:
- `docs/design/phase3.md` (검증 대상, 신규)
- `docs/PLAN.md` v1.0.0
- `docs/PRD.md` v1.0.0
- `CLAUDE.md` (DataPersistence)
- `과제/CLAUDE.md` (루트)

**결과**: 문제 2건 발견 (CRITICAL: 0, WARNING: 2, INFO: 0)

---

## 발견된 문제

### [WARNING-1] update(null) 예외 타입 기술 불일치 — 인터페이스 명세가 상위 문서보다 좁게 확정됨

- **위치**: `docs/design/phase3.md` — 섹션 1.2 메서드 명세 표, 섹션 4 예외 처리 전략 표
- **설명**:
  - `PRD.md` 섹션 6.5는 `save(null)` · `update(null)`에 대해 `NullPointerException` **또는** `IllegalArgumentException` 발생을 허용하며 "구현 시 선택"으로 명시한다.
  - `PLAN.md` Phase 3 섹션 3-3 #13 `update_null_throwsException` 검증 내용도 "`IllegalArgumentException` **또는** `NullPointerException`"으로 기술한다.
  - 반면 `phase3.md` 섹션 1.2 인터페이스 명세 표는 `update(Sample)` 예외를 `IllegalArgumentException` 단일로만 기재하며, 섹션 4 예외 처리 전략 표도 null 인자에 대해 `IllegalArgumentException` 만 표기한다.
  - 구현 코드(섹션 2.5)에서 IAE로 확정한 것은 PRD 6.5 허용 범위 내에서 유효하다. 그러나 인터페이스 명세 표와 예외 전략 표가 NPE 가능성을 언급하지 않아, 상위 문서(PRD 6.5, PLAN.md #13)를 참고하는 독자가 혼란을 겪을 수 있다.
- **권장 조치** (아래 두 안 중 하나 선택):
  - **A안 (현재 구현 유지, 근거 명시)**: phase3.md 섹션 4 예외 처리 전략 표의 "null 인자" 행 비고에 "PRD 6.5의 IAE/NPE 선택 옵션 중 이 PoC는 IAE로 통일"을 한 줄 추가한다.
  - **B안 (상위 문서 표기와 동기화)**: 섹션 1.2 표의 `save`·`update` null 예외 열을 `IllegalArgumentException` (이 구현에서 선택)으로 기재하고, 섹션 4 비고에 동일 한 줄 근거를 추가한다.

---

### [WARNING-2] PLAN.md 내부 모순으로 인한 Phase 2 테스트 수 불일치 — phase3.md 합산 수치에 전파됨

- **위치**:
  - `docs/PLAN.md` — 섹션 2-2 테스트 케이스 표 (10개 나열) vs 파일 생성 순서 요약 주석 ("14개 테스트")
  - `docs/design/phase3.md` — 섹션 6 완료 조건 합산 수치 ("Phase 2 14개" 전제)
- **설명**:
  - `PLAN.md` 섹션 2-2의 `JsonFileUtilTest.java` 테스트 케이스 표에는 케이스가 **10개** 나열되어 있다.
  - 같은 파일 하단 "파일 생성 순서 요약" 주석(`test/.../util/JsonFileUtilTest.java (신규, 14개 테스트)`)과 총 합계(`model(20) + util(14) + repository(13+16) = 63개`)는 **14개**로 기재한다.
  - `phase3.md` 섹션 6 완료 조건은 "Phase 1(20개) + Phase 2(14개) + Phase 3(13개) = 47개"로 PLAN.md의 14개 수치를 그대로 수용한다. Phase 2가 실제 10개라면 합계는 43개가 되어야 한다.
  - 이 모순은 PLAN.md에 먼저 존재하며 phase3.md가 이를 전파한 상태다. phase3.md 단독으로는 PLAN.md 명세를 충실히 따른 것이므로 phase3.md 자체의 귀책은 없다.
- **권장 조치**:
  1. `PLAN.md` 섹션 2-2 표에 실제 구현 예정인 케이스 수를 확인하여 10개와 14개 중 어느 것이 맞는지 확정한다.
  2. 확정된 수치로 `PLAN.md` 섹션 2-2 표, 파일 생성 순서 요약 주석, 총 합계(63개 또는 59개)를 동일하게 수정한다.
  3. `phase3.md` 섹션 6 완료 조건의 Phase 2 수치와 합산 결과를 확정된 값으로 동기화한다.

---

## 통과 항목

| 검증 항목 | 결과 | 비고 |
|----------|------|------|
| [A-1] PLAN.md Phase 3 테스트 케이스 13개 전부 포함 여부 | 이상 없음 | phase3.md 섹션 3.3에 13개 전부 동일 이름으로 포함 |
| [A-2] PLAN.md Phase 3 메서드 명세 일치 | 이상 없음 | save/findById/findAll/update/deleteById 시그니처·예외 일치 |
| [A-3] loadMutable() 헬퍼 근거 | 이상 없음 | PLAN.md 미명시 사항이나 phase3.md 섹션 2.4·5에 설계 근거 명확히 기술 |
| [B-1] PRD 3.1 메서드·예외 명세 일치 | 이상 없음 | 5종 메서드 시그니처와 예외 타입 모두 일치 |
| [B-2] PRD 6.2 테스트 케이스 8개 포함 여부 | 이상 없음 | phase3.md에 PRD 6.2의 8개 케이스 모두 포함 (5개 추가) |
| [B-3] PRD 6.4 픽스처 전략 반영 | 이상 없음 | @TempDir/@BeforeEach 패턴 phase3.md 섹션 3.2에 동일하게 반영 |
| [C-1] 인터페이스 명명 규칙 | 이상 없음 | SampleRepository — I 접두사 없음, 의미 중심 명명 준수 |
| [C-2] 구현체 명명 규칙 | 이상 없음 | JsonSampleRepository — 저장 방식 접두사 준수 |
| [C-3] 생성자 주입 규칙 | 이상 없음 | JsonSampleRepository(Path filePath) 생성자 주입 준수 |
| [C-4] 주석 규칙 | 이상 없음 | WHY 비자명(생성자 주입 이유)에만 한 줄 이내 주석 기재 |
| [C-5] 스코프 제한 (Controller·View 미포함) | 이상 없음 | phase3.md 전체에 Controller, View 관련 내용 없음 |
| [D] 완료 기준 명시 여부 | 이상 없음 | phase3.md 섹션 6에 파일 생성, 테스트 통과, 커버리지 90% 기준 모두 명시 |
| [E-1] 기술 스택 — Java 17 | 이상 없음 | CLAUDE.md·PLAN.md·phase3.md 일치 |
| [E-2] 기술 스택 — Gson 2.11.0 | 이상 없음 | TypeToken 사용 방식 포함 전 문서 일치 |
| [E-3] 기술 스택 — JUnit Jupiter 6.x | 이상 없음 | phase3.md 섹션 3.1에 명시, PLAN.md build.gradle 설정과 일치 |
| [E-4] @TempDir 픽스처 전략 — Phase 2와 동일 | 이상 없음 | Phase 2, Phase 3 동일한 패턴 적용 |
| [E-5] TypeToken 획득 방식 일관성 | 이상 없음 | PLAN.md 3-2절과 phase3.md 섹션 2.2 표기 동일 |
| [E-6] 새 외부 의존성 추가 없음 | 이상 없음 | phase3.md에서 CLAUDE.md에 없는 신규 라이브러리 없음 |
| [F] 패키지 경로 일치 | 이상 없음 | org.ssemi.persistence.repository 전 문서 일치 |
