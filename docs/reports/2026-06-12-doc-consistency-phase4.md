# 문서 정합성 검증 보고서 — Phase 4

**일시**: 2026-06-12
**검증 문서**:
- `docs/design/phase4.md`
- `docs/PLAN.md`
- `docs/PRD.md`
- `DataPersistence/CLAUDE.md`

**결과**: 문제 2건 발견 (CRITICAL: 0, WARNING: 1, INFO: 1)

---

## 발견된 문제

### [WARNING] PLAN.md 16단계 vs phase4.md 완료 조건 "15개 단계" 숫자 불일치

- **위치**: `docs/design/phase4.md` 섹션 3 완료 조건 / `docs/PLAN.md` Phase 4 시나리오 순서
- **설명**:
  PLAN.md Phase 4 시나리오 순서는 항목 1번("JsonSampleRepository, JsonOrderRepository 초기화")부터
  항목 16번("프로그램 재실행 시 data/*.json 복원 안내 메시지")까지 16개 항목을 열거한다.
  반면 phase4.md 섹션 1.3에서는 초기화 항목을 독립 시나리오 단계가 아닌 섹션 1.2(경로 전략) 설명으로
  처리하여 시나리오를 [1]~[15]의 15단계로 구성하였다.
  그 결과 phase4.md 섹션 3 완료 조건의 "./gradlew run — 15개 단계 전체 출력 확인" 문구와
  PLAN.md가 기술한 16개 항목 간 숫자가 일치하지 않는다.
  기능 동작 자체는 누락 없이 모두 포함되어 있으나, 검수 기준 숫자가 달라 혼선이 발생할 수 있다.
- **권장 조치**:
  아래 두 방법 중 하나를 선택한다.
  1. phase4.md 섹션 1.3에 "[0] 초기화 — JsonSampleRepository·JsonOrderRepository 인스턴스 생성" 단계를
     추가하고, 섹션 3 완료 조건을 "16개 단계"로 수정한다.
  2. PLAN.md Phase 4 시나리오 순서 1번(초기화) 항목을 삭제하고 15단계로 통일한다.

---

### [INFO] 섹션 1.5 "선택" 표현과 섹션 1.6 확정 코드 간 표현 불일치

- **위치**: `docs/design/phase4.md` 섹션 1.5 / 섹션 1.6
- **설명**:
  섹션 1.5에서 Files.deleteIfExists 전략을 "선택: 시작 시 data/ 파일 초기화"로 기술하여
  아직 결정되지 않은 옵션처럼 표현하고 있다. 그러나 섹션 1.6 전체 코드에서는 이미
  Files.deleteIfExists 두 줄이 main() 진입부에 확정적으로 포함되어 있어 불일치가 존재한다.
  구현 시 "선택"이 아닌 확정으로 인식해야 하는데, 설계 문서만 읽으면 아직 결정이 열려 있다고
  오해할 수 있다.
- **권장 조치**:
  섹션 1.5의 "선택: 시작 시 data/ 파일 초기화" 표현을 "확정: 시작 시 data/ 파일 초기화"로
  수정하거나, 도입부를 "아래 전략을 채택한다"로 변경하여 결정이 완료된 사항임을 명확히 한다.

---

## 통과 항목

- [A] 교차 참조: 이상 없음 — PLAN.md Phase 0~4 전부 대응 파일 존재, 내부 링크 오류 없음
- [B] 기술 스택: 이상 없음 — Gson 2.11+, JUnit Jupiter 6.x, Java 17, Gradle 8.x 문서 간 일치, 신규 의존성 없음
- [C] 설계 제약 반영: 이상 없음 — phase4.md 코드에 Controller·View·비즈니스 로직 없음, CLAUDE.md 스코프 제한 준수
- [D] 완료 기준: 이상 없음 — phase4.md 섹션 3에 7개 완료 조건 명시, PLAN.md 및 PRD 섹션 10 항목 모두 포함
- [E] 내부 모순: 이상 없음 — 섹션 1.3 단계 표와 섹션 1.6 코드의 단계 번호·동작 일치, 픽스처 데이터가 PRD 섹션 5 JSON 포맷 예시와 일치, data/ 경로 전략이 PLAN.md 리스크 대응과 일치, Files.deleteIfExists 논리 타당
