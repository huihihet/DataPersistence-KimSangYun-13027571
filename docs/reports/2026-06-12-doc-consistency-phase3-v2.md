# 문서 정합성 검증 보고서 — Phase 3 v1.1.0 (PLAN.md 통합 개정 후)

**일시**: 2026-06-12
**검증 문서**:
- `docs/design/phase3.md` v1.1.0 (SampleRepository + OrderRepository 통합)
- `docs/PLAN.md` v1.0.0 (Phase 3~4로 통합 개정)
- `docs/PRD.md` v1.0.0
- `CLAUDE.md` (DataPersistence)

**결과**: 문제 3건 발견 (CRITICAL: 1, WARNING: 2, INFO: 0)

---

## 발견된 문제

### [CRITICAL] phase4.md 파일 미존재

- **위치**: `docs/PLAN.md` — Phase 4 섹션, 파일 생성 순서 요약
- **설명**: PLAN.md에 Phase 4 (`Main 시나리오 스크립트 + 빌드 & 커버리지 검증`) 가 기술되어 있고, 파일 생성 순서 요약에도 `Phase 4  Main.java (신규)` 가 명시되어 있다. 그러나 `docs/design/` 디렉터리에 `phase4.md` 파일이 존재하지 않는다. 확인된 파일: `phase1.md`, `phase2.md`, `phase3.md` 세 개뿐이다.
- **권장 조치**: `docs/design/phase4.md`를 작성한다. PLAN.md Phase 4 섹션(Main 시나리오 순서 16단계, 빌드·커버리지 검증 명령, 완료 조건)을 기반으로 `Main.java` 구현 명세와 완료 기준(acceptance criteria)을 포함해야 한다.

---

### [WARNING-1] PRD 3.2 findByStatus 예외 명세 — null 인자 정책 누락 (PRD 내부 모순)

- **위치**: `docs/PRD.md` — 섹션 3.2 OrderRepository 테이블 vs 섹션 6.5 null 인자 정책
- **설명**: PRD 섹션 6.5에는 `findByStatus(null) → IllegalArgumentException 발생` 이 명시되어 있다. 그러나 섹션 3.2의 `findByStatus(OrderStatus)` 예외 처리 칸은 `빈 리스트 반환` 만 기술되어 있으며 null 인자 예외가 누락되어 있다. 두 섹션 간 내부 모순이 존재한다.
  - PRD 3.2 `findByStatus` 예외 처리 칸: `빈 리스트 반환` (null 예외 미언급)
  - PRD 6.5: `findByStatus(null) → IllegalArgumentException`
  - phase3.md 섹션 3.2 / 4.3: `status == null → IllegalArgumentException` — PRD 6.5 기준으로 올바르게 구현됨
- **권장 조치**: PRD 3.2 `findByStatus` 행의 예외 처리 칸을 `빈 리스트 반환; null 인자 시 IllegalArgumentException` 으로 수정하여 섹션 6.5와 일치시킨다.

---

### [WARNING-2] PLAN.md Phase 2 테스트 케이스 목록 불완전 — 완료 조건(14개)과 나열 수(10개) 불일치

- **위치**: `docs/PLAN.md` — 섹션 2-2 `JsonFileUtilTest.java` 테스트 케이스 표 vs Phase 2 완료 조건 vs 파일 생성 순서 요약
- **설명**: Phase 2 완료 조건에 `JsonFileUtilTest 14개 테스트 모두 통과` 라고 명시되어 있고, 파일 생성 순서 요약의 총합 계산(`model(20) + util(14) + repository(29) = 63개`)에도 14개가 사용된다. 그러나 섹션 2-2의 케이스 목록 표에는 #1~#10, 총 10개만 나열되어 있으며 나머지 4개 케이스 내용이 기술되어 있지 않다.
  - 섹션 2-2 표 나열 수: 10개
  - Phase 2 완료 조건 선언: 14개
  - phase3.md 섹션 8 합산 근거: Phase 2를 14개로 산정하여 총 63개 계산
- **권장 조치**: 아래 두 안 중 하나를 선택한다.
  - **A안 (14개가 올바른 경우)**: PLAN.md 섹션 2-2 표에 누락된 4개 케이스를 추가 기술하고, `docs/design/phase2.md` 테스트 케이스 목록도 동기화한다.
  - **B안 (10개가 올바른 경우)**: PLAN.md 완료 조건의 `14개`를 `10개`로 수정하고, 파일 생성 순서 요약 주석(`14개 테스트`)·총합(`63개`)도 `10개`·`59개`로 수정한다. 이 경우 phase3.md 섹션 8의 합산(`20+14+29=63`)도 `20+10+29=59`로 수정해야 한다.

---

## 통과 항목

| 검증 항목 | 결과 | 비고 |
|----------|------|------|
| [A] PLAN.md Phase 3 파일 생성 목록과 phase3.md 섹션 생성 파일 일치 | 이상 없음 | 6개 파일 경로 완전 일치 |
| [A] PLAN.md 3-5 케이스명 13개 vs phase3.md 섹션 5.3 | 이상 없음 | 케이스명·순서 완전 일치 |
| [A] PLAN.md 3-6 케이스명 16개 vs phase3.md 섹션 6.3 | 이상 없음 | 케이스명·순서 완전 일치 |
| [B] 기술 스택 — Gson 2.11.0 | 이상 없음 | CLAUDE.md·PRD·PLAN·phase3.md 전 문서 일치 |
| [B] 기술 스택 — JUnit Jupiter 6.x | 이상 없음 | 전 문서 일치 |
| [B] 기술 스택 — Java 17 | 이상 없음 | 전 문서 일치 |
| [B] 신규 외부 의존성 추가 없음 | 이상 없음 | phase3.md에서 CLAUDE.md 미등재 라이브러리 없음 |
| [C] CLAUDE.md 설계 제약 — Controller/View 미포함 | 이상 없음 | phase3.md 전체에 Controller·View 내용 없음 |
| [C] CLAUDE.md 인터페이스 명명 규칙 | 이상 없음 | I 접두사 없음, SampleRepository·OrderRepository |
| [C] CLAUDE.md 구현체 명명 규칙 | 이상 없음 | Json 접두사 사용, JsonSampleRepository·JsonOrderRepository |
| [C] CLAUDE.md 생성자 주입 규칙 | 이상 없음 | JsonSampleRepository(Path), JsonOrderRepository(Path) 생성자 주입 |
| [C] CLAUDE.md 주석 규칙 | 이상 없음 | WHY 비자명(생성자 주입 이유)에만 한 줄 이내 기재 |
| [D] 완료 기준(acceptance criteria) 명시 | 이상 없음 | phase3.md 섹션 8에 체크리스트 형태로 완료 조건 명시 |
| [E] PRD 3.1 SampleRepository 메서드·예외 명세 vs phase3.md 섹션 1.2 | 이상 없음 | 5종 메서드 시그니처·예외 타입 일치 |
| [E] PRD 3.2 OrderRepository 메서드·예외 명세 vs phase3.md 섹션 3.2 | 이상 없음 | 6종 메서드 시그니처·예외 타입 일치 (findByStatus 포함) |
| [E] PRD 6.5 null 정책 vs phase3.md 섹션 2.3 IAE 통일 선언 | 이상 없음 | PRD 6.5 허용 범위(IAE or NPE) 내에서 IAE로 확정, 모순 없음 |
| [E] findByStatus 동작 — PRD 6.5 vs phase3.md 섹션 4.3 | 이상 없음 | null → IAE, 일치 없으면 빈 리스트 반환 일치 |
| [E] 테스트 수 합계 — phase3.md 완료 조건 vs PLAN.md Phase 4 완료 조건 | 이상 없음 | 양측 모두 63개로 일치 (14개 전제 하에) |
| [E] 패키지 경로 일치 | 이상 없음 | org.ssemi.persistence.repository 전 문서 일치 |
| [E] @TempDir 픽스처 전략 | 이상 없음 | PRD 6.4, PLAN.md, phase3.md 섹션 5.2·6.2 일치 |
| [F] phase3.md 섹션 9 Phase 4 연계 기술 | 이상 없음 | Phase 4에서 두 Repository 직접 호출 시연 명시 |
