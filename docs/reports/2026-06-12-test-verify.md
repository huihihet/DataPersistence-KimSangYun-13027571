# 테스트 전략 검증 보고서

**일시**: 2026-06-12
**검증 대상**: `DataPersistence/docs/PRD.md` (섹션 6 테스트 전략)
**결과**: 미흡 11건 (CRITICAL: 4, WARNING: 7)

---

## 발견된 문제

### [CRITICAL-1] OrderRepository 테스트 케이스 대거 누락

- **대상 기능**: `JsonOrderRepository` (PRD 섹션 3.2)
- **문제**: PRD 섹션 6.3 `JsonOrderRepositoryTest` 표에 다음 케이스가 전혀 없다.
  - `deleteById` 존재하는 ID — 삭제 후 `findById` → `Optional.empty()` 확인
  - `deleteById` 없는 ID — `NoSuchElementException` 발생 확인
  - `update` 없는 ID — `NoSuchElementException` 발생 확인
  - `findById` 없는 ID — `Optional.empty()` 반환 확인
  - `findAll` 초기 빈 리스트 반환 확인
  반면 `JsonSampleRepositoryTest`(섹션 6.2)에는 위 케이스가 모두 포함되어 있어 두 테스트 클래스 간 커버리지가 비대칭이다.
- **권장 테스트**:
  ```
  deleteById_존재() → findById 결과 empty 확인
  deleteById_없는ID() → assertThrows(NoSuchElementException.class, ...)
  update_없는ID() → assertThrows(NoSuchElementException.class, ...)
  findById_없는ID() → assertTrue(result.isEmpty())
  findAll_초기() → assertEquals(0, repo.findAll().size())
  ```

---

### [CRITICAL-2] null 입력 처리 동작 미명시 및 테스트 없음

- **대상 기능**: `JsonSampleRepository`, `JsonOrderRepository`, `JsonFileUtil`
- **문제**: PRD 기능 요구사항(섹션 3.1, 3.2)과 테스트 계획(섹션 6) 모두 `null` 입력에 대한 동작을 정의하지 않는다.
  - `save(null)` — NPE 발생? `IllegalArgumentException` 발생?
  - `findById(null)` — `Optional.empty()` 반환? NPE?
  - `deleteById(null)` — `NoSuchElementException`? NPE?
  - `findByStatus(null)` — 빈 리스트? NPE?
  동작이 미정의이면 구현자마다 다르게 구현할 수 있고, SSemi 이식 시 예기치 않은 NPE가 발생할 수 있다.
- **권장 조치**: PRD 섹션 3.1~3.2 예외 처리 열에 null 인자 동작을 명시하고, 섹션 6에 해당 케이스를 추가한다.
  ```
  save_null() → assertThrows(IllegalArgumentException.class, ...) 또는 NullPointerException
  findById_null() → assertTrue(result.isEmpty()) 또는 assertThrows(...)
  findByStatus_null() → assertThrows(IllegalArgumentException.class, ...)
  ```

---

### [CRITICAL-3] 손상된 JSON 파일(malformed JSON) 처리 테스트 누락

- **대상 기능**: `JsonFileUtil.readAll` (PRD 섹션 3.3)
- **문제**: 비기능 요구사항(섹션 4)은 "파일 없을 경우 빈 리스트 반환"만 명시한다. 파일이 존재하지만 내용이 올바르지 않은 JSON(`{broken`, 빈 문자열 등)인 경우 Gson은 `JsonSyntaxException`을 던지는데, Repository 또는 Util 레벨에서 이를 어떻게 처리해야 하는지 PRD와 테스트 계획 어디에도 정의되어 있지 않다. 프로덕션 데이터 파일 손상 시 애플리케이션 전체가 비정상 종료될 수 있다.
- **권장 테스트**:
  ```
  readAll_손상된JSON파일() → assertThrows(JsonSyntaxException.class, ...) 또는 예외를 잡아 빈 리스트 반환하는 정책 명시 후 테스트
  readAll_빈문자열파일() → 동작 명시 및 테스트
  ```

---

### [CRITICAL-4] data/ 디렉터리 자동 생성 동작 테스트 누락

- **대상 기능**: `JsonFileUtil.writeAll` 또는 Repository 초기화 (PRD 섹션 3.3, 비기능 요구사항 섹션 4)
- **문제**: PRD 비기능 요구사항에 "파일 없을 경우 빈 배열로 자동 초기화"가 명시되어 있으나, `data/` 디렉터리 자체가 존재하지 않는 경우(`writeAll` 첫 호출 시)에 대한 처리와 테스트가 없다. `@TempDir`을 사용하는 테스트 환경에서는 부모 디렉터리가 항상 존재하지만, 실제 `data/` 경로는 다를 수 있다.
- **권장 테스트**:
  ```
  writeAll_부모디렉터리없음() → 디렉터리 자동 생성 후 정상 기록 확인, 또는 IOException 발생 명시
  ```

---

### [WARNING-1] findByStatus 결과 빈 리스트 케이스 누락

- **대상 기능**: `JsonOrderRepository.findByStatus` (PRD 섹션 3.2)
- **문제**: PRD 섹션 6.3의 `findByStatus` 케이스는 "상태별 필터링 정확성" 하나만 있다. 해당 상태의 주문이 하나도 없을 때 빈 리스트를 반환하는지 확인하는 케이스가 없다. PRD 섹션 3.2 예외 처리에 "빈 리스트 반환"이 명시되어 있으므로 이를 반드시 검증해야 한다.
- **권장 테스트**:
  ```
  findByStatus_해당상태없음() → assertEquals(0, repo.findByStatus(CONFIRMED).size())
  findByStatus_복수건() → 저장한 건수와 반환 건수 일치 확인
  findByStatus_전체enum값() → 5개 상태 각각에 대해 정확히 필터링되는지 확인
  ```

---

### [WARNING-2] OrderStatus enum 전체 직렬화·역직렬화 라운드트립 테스트 없음

- **대상 기능**: Gson의 `OrderStatus` enum 직렬화 (PRD 섹션 2.3, 3.2)
- **문제**: Gson은 기본적으로 enum을 이름 문자열로 직렬화하나, 커스텀 어댑터 설정에 따라 달라질 수 있다. PRD 섹션 5 JSON 포맷 예시에 `"status": "RESERVED"` 형태가 있지만, 5개 상태값 모두의 라운드트립(save → 파일 쓰기 → 파일 읽기 → 상태값 일치)을 검증하는 케이스가 없다.
- **권장 테스트**:
  ```
  save_PRODUCING_findById() → status == PRODUCING 확인
  save_CONFIRMED_findById() → status == CONFIRMED 확인
  save_RELEASE_findById() → status == RELEASE 확인
  save_REJECTED_findById() → status == REJECTED 확인
  ```

---

### [WARNING-3] 경계값(yield=0.0, yield=1.0, stock=0) 직렬화 검증 없음

- **대상 기능**: `JsonSampleRepository`, `JsonFileUtil`
- **문제**: `Sample.yield`는 `0.0~1.0` 범위 값이고 `stock`은 `0`이 유효한 값이다. 경계값이 JSON 직렬화 후 정밀도 손실 없이 복원되는지 검증하는 케이스가 없다. double 타입은 JSON 직렬화 시 부동소수점 표현 이슈가 생길 수 있다.
- **권장 테스트**:
  ```
  save_yield0p0_findById() → yield == 0.0 확인 (assertEquals(0.0, sample.getYield(), 1e-9))
  save_yield1p0_findById() → yield == 1.0 확인
  save_stock0_findById() → stock == 0 확인
  ```

---

### [WARNING-4] 커버리지 목표 불일치 — CLAUDE.md(80%) vs PRD.md(90%)

- **대상**: DataPersistence CLAUDE.md 기술 스택 섹션 vs PRD.md 섹션 6.4 및 완료 조건 섹션 10
- **문제**: DataPersistence CLAUDE.md에는 "Repository·Util 핵심 로직 **80% 이상**"으로 명시되어 있고, PRD.md 섹션 6.4와 섹션 10 완료 조건에는 "**90% 이상**"으로 명시되어 있다. 어느 기준이 구속력을 갖는지 명시되지 않았다.
  - doc-consistency 보고서(2026-06-12)에서도 이 불일치를 인지했으나 별도 지적 항목으로 다루지 않았다.
- **권장 조치**: DataPersistence CLAUDE.md 커버리지 목표를 PRD.md에 맞춰 90%로 상향하거나, PRD.md를 80%로 하향하여 단일 기준으로 통일한다.

---

### [WARNING-5] @TempDir 픽스처 설정 방식 미명시

- **대상**: PRD.md 섹션 6 전체
- **문제**: 비기능 요구사항(섹션 4)에 "@TempDir 사용, data/ 실제 파일 오염 금지"가 명시되어 있으나, 테스트 섹션(섹션 6)에는 @TempDir을 각 테스트 메서드마다 생성할지, @BeforeEach에서 공유 경로를 설정할지, Repository 생성자에 경로를 주입하는 방식인지 기술되지 않았다. 이 설계가 구현 방식을 구속하기 때문에 명시가 필요하다.
- **권장 조치**: 섹션 6 도입부에 공통 픽스처 전략을 추가한다.
  ```
  @TempDir Path tempDir;
  // @BeforeEach에서 tempDir 기반 경로로 Repository 인스턴스 생성
  // 각 테스트는 독립된 임시 파일 경로를 사용하여 상호 오염 방지
  ```

---

### [WARNING-6] 테스트 실행 순서 독립성 보장 방법 미명시

- **대상**: PRD.md 섹션 6 전체
- **문제**: JSON Repository는 파일 상태에 의존하기 때문에, 테스트 실행 순서에 따라 이전 테스트가 기록한 파일이 다음 테스트에 영향을 줄 수 있다. @TempDir 사용이 명시되어 있지만, @BeforeEach에서 파일을 초기화하는 전략이나 각 테스트별 독립 경로 전략이 테스트 계획에 없다.
- **권장 조치**: 섹션 6에 "각 테스트 메서드는 새로운 @TempDir 경로 또는 @BeforeEach 초기화를 통해 독립 실행을 보장한다"는 문장을 추가한다.

---

### [WARNING-7] writeAll pretty print 포맷 검증 테스트 없음

- **대상 기능**: `JsonFileUtil.writeAll` (PRD 섹션 3.3, 비기능 요구사항 섹션 4)
- **문제**: 비기능 요구사항에 "들여쓰기 적용(pretty print)"이 명시되어 있으나, 실제 출력 파일이 들여쓰기 포맷을 갖추는지 검증하는 케이스가 없다. Gson의 `GsonBuilder.setPrettyPrinting()` 설정 누락 시 한 줄 JSON이 출력되어 가독성 요건을 위반한다.
- **권장 테스트**:
  ```
  writeAll_prettyPrint() → 파일 내용에 줄바꿈 또는 들여쓰기 문자 포함 확인
  // Files.readString(path)으로 파일 내용 읽어 "\n" 또는 "  " 포함 여부 assertTrue
  ```

---

## 검증 결과 요약

- [A] 테스트 계획 존재: 부분 통과 — 섹션 6에 테스트 표가 있으나 OrderRepository 케이스 5종 누락
- [B] 엣지케이스 식별: 미흡 — null 입력, 손상된 JSON, 디렉터리 미존재, 경계값(yield/stock), enum 전체값 라운드트립 미포함
- [C] 기존 테스트 충돌: 통과 — 현재 구현 파일 없음, 루트 CLAUDE.md와 직접 충돌 없음 (단, 커버리지 목표 수치 불일치는 WARNING-4로 별도 기록)
- [D] 테스트 구조: 부분 통과 — @TempDir 격리 정책 비기능 요구사항에 명시되나 테스트 픽스처·초기화 방법은 미기술
