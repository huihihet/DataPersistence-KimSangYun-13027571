# 테스트 전략 검증 보고서

**일시**: 2026-06-12
**검증 대상**: `docs/design/phase2.md` (JsonFileUtil 설계)
**결과**: 미흡 4건 (CRITICAL: 2, WARNING: 2)

---

## 발견된 문제

### [CRITICAL-1] UncheckedIOException 전파 계약 테스트 부재

- **대상 기능**: `JsonFileUtil.writeAll` / `readAll` — IOException 처리 전략 (섹션 3)
- **문제**: 설계 문서 섹션 3에서 `IOException`을 `UncheckedIOException`으로 래핑해 전파한다고 명시하고, Phase 3·4 연계 계약(섹션 4)에서 Repository가 이를 신뢰해야 한다고 기술하고 있으나, 이 계약을 실제로 검증하는 테스트 케이스가 없다. 연계 계약 표(섹션 4)에도 이 불변식이 누락되어 있다. Repository가 `UncheckedIOException`을 처리하지 않거나 예상치 못한 checked `IOException`이 전파될 경우 Phase 3·4 전체 빌드가 실패한다.
- **권장 테스트**:
  ```
  테스트명: writeAll_ioError_throwsUncheckedIOException
  Given: 파일시스템이 쓰기를 거부하는 경로 (e.g., 존재하는 디렉토리와 동일한 이름의 경로)
  When:  JsonFileUtil.writeAll(invalidPath, List.of(sample)) 호출
  Then:  assertThrows(UncheckedIOException.class, ...)
  ```
  Phase 3·4 연계 계약 표에도 다음 불변식을 추가해야 한다:
  "IOException 발생 시 UncheckedIOException으로 래핑해 전파 — Phase 3·4 Repository에서 checked 예외 선언 불필요"

### [CRITICAL-2] JSON 배열이 아닌 내용(단일 객체, 원시값 등) readAll 동작 미검증

- **대상 기능**: `JsonFileUtil.readAll` — 손상 JSON 처리
- **문제**: 설계 문서 1.4절에서 손상 JSON은 `JsonSyntaxException` 전파, 정상 JSON 배열은 역직렬화라고 이분법으로 기술한다. 그러나 파일 내용이 문법적으로는 유효한 JSON이지만 배열이 아닌 경우(e.g., `{"key":"value"}` 또는 숫자 `42`)의 동작이 정의되지 않았다. Gson 버전에 따라 `null`을 반환하거나 `JsonSyntaxException`을 발생시킬 수 있어 Phase 3·4 Repository가 예상 외의 `null` 리스트를 수신해 `NullPointerException`으로 이어질 수 있다. 구현 코드(섹션 1.5)에서 `result != null ? result : Collections.emptyList()`로 방어하고 있으나, 이를 검증하는 테스트가 없어 계약으로서 보장되지 않는다.
- **권장 테스트**:
  ```
  테스트명: readAll_validJsonButNotArray_returnsEmptyListOrThrows
  Given: 파일 내용 = "{\"sampleId\":\"S001\"}" (유효한 JSON 객체, 배열 아님)
  When:  JsonFileUtil.readAll(path, sampleListType) 호출
  Then:  result.isEmpty() == true  (또는 명시적으로 JsonSyntaxException을 던지도록 설계 결정 필요)
  ```
  이 케이스에 대한 동작을 섹션 1.4의 readAll 단계 표에 추가하고 Phase 3·4 연계 계약에 불변식으로 포함해야 한다.

### [WARNING-1] double delta=0.0 사용 근거 미명시로 인한 오용 위험

- **대상 기능**: 테스트 #7 `writeAll_doubleEdgeValues_noLoss` — double 정밀도 검증
- **문제**: 설계 문서 섹션 2.3과 2.4에서 `assertEquals(0.0, loaded.getYield(), 0.0)` 형태로 delta=0.0을 사용한다. `0.0`과 `1.0`은 IEEE 754 이진 부동소수점에서 정확히 표현 가능하므로 이 정책은 #7에 한해 수학적으로 안전하다. 그러나 설계 문서가 이 근거를 명시하지 않아 구현자가 `0.85`, `0.92` 등 정확히 표현되지 않는 수율값에도 동일 패턴(delta=0.0)을 적용할 가능성이 있다. 향후 #2번 테스트(`writeAll_thenReadAll_returnsSameData`)에서 `yield=0.85`를 사용하는 경우 delta=0.0이 산발적 실패를 유발할 수 있다.
- **권장 조치**:
  섹션 2.4의 #7 시나리오 블록에 다음 주석을 추가한다:
  ```
  // 0.0, 1.0은 IEEE 754 이진 부동소수점으로 정확히 표현 가능하므로 delta=0.0 사용
  // 그 외 수율값(e.g., 0.85)은 assertEquals(expected, actual, 1e-9) 형태의 허용 오차 사용
  ```
  또한 #2 테스트 시나리오에서 `yield=0.85` 사용 시 delta 정책을 명시해야 한다.

### [WARNING-2] 파일 내용이 공백 문자열인 경우 readAll 직접 검증 케이스 부재

- **대상 기능**: `JsonFileUtil.readAll` — 빈/공백 내용 처리
- **문제**: 구현 명세 1.4절에서 "파일 내용이 빈 문자열 또는 `[]` (trim 후 비교)"를 처리한다고 명시되어 있다. 그러나 테스트 케이스 중 이 경로를 직접 검증하는 케이스가 없다. #3(`writeAll_emptyList_writesEmptyArray`)은 `writeAll`이 `[]`를 기록하는지를 검증하지만, 외부에서 공백 문자열이나 빈 파일을 주입한 뒤 `readAll`이 빈 리스트를 반환하는지는 별도로 검증되지 않는다. `trim()` 처리가 실수로 제거될 경우 탐지할 수 없다.
- **권장 테스트**:
  ```
  테스트명: readAll_emptyFileContent_returnsEmptyList
  Given: 파일 내용 = "" (빈 문자열) 또는 "   \n  " (공백만 있는 파일)
  When:  JsonFileUtil.readAll(path, sampleListType) 호출
  Then:  result.isEmpty() == true, 예외 없음

  테스트명: readAll_emptyArrayContent_returnsEmptyList
  Given: 파일 내용 = "[]"
  When:  JsonFileUtil.readAll(path, sampleListType) 호출
  Then:  result.isEmpty() == true, 예외 없음
  ```

---

## 검증 결과 요약

| 항목 | 결과 | 비고 |
|------|------|------|
| [A] 테스트 계획 존재 | 통과 | 10개 케이스 표 + 개별 시나리오 블록 모두 존재 |
| [B] 엣지케이스 식별 | 미흡 | 비배열 JSON, 공백 파일 내용 케이스 누락 |
| [C] 기존 테스트 충돌 | 통과 | Phase 1 테스트(SampleTest 10개, OrderTest 10개)와 충돌 없음 |
| [D] 테스트 구조 적절성 | 부분 미흡 | UncheckedIOException 계약 검증 구조 부재, Phase 3·4 연계 계약 표 불완전 |

### PRD.md 섹션 6.1 대조 결과

| PRD 6.1 케이스 | phase2.md 포함 | 비고 |
|---|---|---|
| 파일 없을 때 readAll | 포함 (#1) | |
| 객체 저장 후 readAll | 포함 (#2) | |
| 빈 리스트 writeAll | 포함 (#3) | |
| 한글 포함 문자열 | 포함 (#4) | |
| 손상된 JSON readAll | 포함 (#5) | 예외 타입 JsonSyntaxException으로 구체화됨 |
| data/ 디렉토리 미존재 시 writeAll | 포함 (#6) | |
| double 경계값(0.0, 1.0) 직렬화 | 포함 (#7) | delta=0.0 근거 미명시 (WARNING-1) |
| readAll null 경로 (#8) | 포함 | PLAN.md 기준 — PRD 6.1 테이블에는 별도 행 없음 |
| writeAll null 경로 (#9) | 포함 | PLAN.md 기준 |
| writeAll null 리스트 (#10) | 포함 | PLAN.md 기준 |
