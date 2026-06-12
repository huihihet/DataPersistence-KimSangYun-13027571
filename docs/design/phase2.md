# Phase 2 — JsonFileUtil 설계

**버전**: 1.0.0
**작성일**: 2026-06-12
**기반 문서**: `docs/PLAN.md` Phase 2, `docs/PRD.md` 섹션 3.3·4·6.1
**선행 Phase**: Phase 1 (도메인 모델) 완료 필요
**상태**: 초안

---

## 목표

JSON 파일 읽기·쓰기 공통 유틸(`JsonFileUtil`)을 구현하고 단위 테스트로 완전히 검증한다.
Phase 3·4의 `JsonSampleRepository`·`JsonOrderRepository`가 이 유틸에 의존하므로
가장 먼저 안정화해야 한다.

---

## 생성 파일

```
src/main/java/org/ssemi/persistence/util/JsonFileUtil.java
src/test/java/org/ssemi/persistence/util/JsonFileUtilTest.java
```

---

## 1. JsonFileUtil

### 1.1 파일 정보

| 항목 | 값 |
|------|-----|
| 파일 경로 | `src/main/java/org/ssemi/persistence/util/JsonFileUtil.java` |
| 종류 | `class` (인스턴스화 불가) |
| 패키지 | `org.ssemi.persistence.util` |

### 1.2 클래스 구조 결정

`JsonFileUtil`은 상태를 갖지 않는 순수 파일 유틸이다.

| 항목 | 결정 | 근거 |
|------|------|------|
| 인스턴스화 | `private` 생성자로 차단 | 유틸 클래스 관용 패턴; 인스턴스 생성이 무의미 |
| 메서드 | `static` | 상태 없음; Repository에서 `new` 없이 호출 가능 |
| Gson 인스턴스 | `static final` 필드 | pretty print 설정을 한 곳에서 관리 |

### 1.3 Gson 설정

```java
private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
```

`setPrettyPrinting()`을 사용하는 이유: `data/*.json`을 사람이 직접 읽고 확인할 수 있어야 하기 때문이다 (PRD.md 섹션 4).

### 1.4 메서드 명세

#### `readAll`

```java
public static <T> List<T> readAll(Path filePath, Type listType)
```

| 단계 | 조건 | 동작 |
|------|------|------|
| 1 | `filePath == null` | `IllegalArgumentException` 발생 |
| 2 | 파일 미존재 (`!Files.exists(filePath)`) | `Collections.emptyList()` 반환 |
| 3 | 파일 내용이 빈 문자열 또는 `"[]"` (trim 후 비교) | `Collections.emptyList()` 반환 |
| 4 | JSON 파싱 실패 | `JsonSyntaxException` 그대로 전파 (래핑 금지) |
| 5 | JSON은 유효하나 배열이 아닌 형태 (단일 객체 등) | `Collections.emptyList()` 반환 (`fromJson` 결과 `null` 방어) |
| 6 | 정상 JSON 배열 | `GSON.fromJson(content, listType)` 결과 반환 |

파일 읽기: `Files.readString(filePath, StandardCharsets.UTF_8)`

#### `writeAll`

```java
public static <T> void writeAll(Path filePath, List<T> items)
```

| 단계 | 조건 | 동작 |
|------|------|------|
| 1 | `filePath == null` | `IllegalArgumentException` 발생 |
| 2 | `items == null` | `IllegalArgumentException` 발생 |
| 3 | 부모 디렉토리 미존재 | `Files.createDirectories(filePath.getParent())` 호출 |
| 4 | `IOException` 발생 시 | `UncheckedIOException`으로 래핑 후 전파 |
| 5 | 항상 | `Files.writeString(filePath, GSON.toJson(items), StandardCharsets.UTF_8)` (전체 덮어쓰기, `APPEND` 금지) |

### 1.5 전체 코드

```java
package org.ssemi.persistence.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class JsonFileUtil {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private JsonFileUtil() {}

    public static <T> List<T> readAll(Path filePath, Type listType) {
        if (filePath == null) throw new IllegalArgumentException("filePath must not be null");
        if (!Files.exists(filePath)) return Collections.emptyList();
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8).trim();
            if (content.isEmpty() || content.equals("[]")) return Collections.emptyList();
            List<T> result = GSON.fromJson(content, listType);
            return result != null ? result : Collections.emptyList();
        } catch (JsonSyntaxException e) {
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> void writeAll(Path filePath, List<T> items) {
        if (filePath == null) throw new IllegalArgumentException("filePath must not be null");
        if (items == null) throw new IllegalArgumentException("items must not be null");
        try {
            Path parent = filePath.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(filePath, GSON.toJson(items), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
```

---

## 2. JsonFileUtilTest

### 2.1 파일 정보

| 항목 | 값 |
|------|-----|
| 파일 경로 | `src/test/java/org/ssemi/persistence/util/JsonFileUtilTest.java` |
| 패키지 | `org.ssemi.persistence.util` |
| 테스트 프레임워크 | JUnit Jupiter 6.x |

### 2.2 픽스처 전략

- 클래스 레벨: `@TempDir Path tempDir` — JUnit이 테스트별 독립 임시 디렉토리를 주입
- 직렬화 대상: `Sample` 클래스를 픽스처 객체로 활용 (Phase 1 산출물)
- `Type` 획득: `new TypeToken<List<Sample>>() {}.getType()`

### 2.3 테스트 케이스

| # | 테스트명 | 검증 내용 | 핵심 assert |
|---|---------|-----------|------------|
| 1 | `readAll_fileNotExists_returnsEmptyList` | 파일 없을 때 빈 리스트, 예외 없음 | `assertTrue(result.isEmpty())` |
| 2 | `writeAll_thenReadAll_returnsSameData` | 저장 후 동일 데이터 역직렬화 | `assertEquals(saved, loaded)` |
| 3 | `writeAll_emptyList_writesEmptyArray` | 빈 리스트 → 파일 내용 `[]` 포함 | `assertTrue(content.contains("[]"))` |
| 4 | `writeAll_koreanString_utf8Preserved` | 한글 문자열 UTF-8 손실 없음 | `assertEquals("GaN 웨이퍼", loaded.getName())` |
| 5 | `readAll_corruptedJson_throwsException` | 손상 JSON → `JsonSyntaxException` | `assertThrows(JsonSyntaxException.class, ...)` |
| 6 | `writeAll_dirNotExists_createsDir` | 하위 디렉토리 자동 생성 후 파일 기록 | `assertTrue(Files.exists(nestedPath))` |
| 7 | `writeAll_doubleEdgeValues_noLoss` | `yield=0.0`·`1.0` 정밀도 손실 없음 | `assertEquals(0.0, loaded.getYield(), 0.0)` 등 |
| 8 | `readAll_nullPath_throwsException` | `null` 경로 → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| 9 | `writeAll_nullPath_throwsException` | `null` 경로 → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| 10 | `writeAll_nullItems_throwsException` | `null` 리스트 → `IllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| 11 | `readAll_blankFileContent_returnsEmptyList` | 공백만 있는 파일 → 빈 리스트 | `assertTrue(result.isEmpty())` |
| 12 | `readAll_emptyArrayContent_returnsEmptyList` | `[]` 내용 파일 → 빈 리스트 | `assertTrue(result.isEmpty())` |
| 13 | `readAll_validJsonObject_returnsEmptyList` | 유효 JSON이나 배열 아닌 경우 → 빈 리스트 | `assertTrue(result.isEmpty())` |
| 14 | `writeAll_ioError_throwsUncheckedIOException` | 쓰기 거부 경로 → `UncheckedIOException` | `assertThrows(UncheckedIOException.class, ...)` |

### 2.4 테스트 케이스별 상세 시나리오

**#1 `readAll_fileNotExists_returnsEmptyList`**
```
Path path = tempDir.resolve("nonexistent.json")
List<Sample> result = JsonFileUtil.readAll(path, type)
→ result.isEmpty() == true
```

**#2 `writeAll_thenReadAll_returnsSameData`**
```
Sample s = new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 50)
Path path = tempDir.resolve("samples.json")
JsonFileUtil.writeAll(path, List.of(s))
List<Sample> loaded = JsonFileUtil.readAll(path, type)
→ loaded.size() == 1
→ loaded.get(0).equals(s)   // sampleId 기준 equals
→ loaded.get(0).getStock() == 50
```

**#3 `writeAll_emptyList_writesEmptyArray`**
```
Path path = tempDir.resolve("empty.json")
JsonFileUtil.writeAll(path, Collections.emptyList())
String content = Files.readString(path)
→ content.contains("[]") == true
```

**#4 `writeAll_koreanString_utf8Preserved`**
```
Sample s = new Sample("S001", "GaN 웨이퍼", 0, 0.0, 0)
JsonFileUtil.writeAll(path, List.of(s))
Sample loaded = JsonFileUtil.readAll(path, type).get(0)
→ loaded.getName().equals("GaN 웨이퍼") == true
```

**#5 `readAll_corruptedJson_throwsException`**
```
Path path = tempDir.resolve("bad.json")
Files.writeString(path, "{ not valid json [[[")
→ assertThrows(JsonSyntaxException.class,
       () -> JsonFileUtil.readAll(path, type))
```

**#6 `writeAll_dirNotExists_createsDir`**
```
Path nested = tempDir.resolve("a/b/c/samples.json")
// a/b/c/ 디렉토리 없는 상태에서 호출
JsonFileUtil.writeAll(nested, List.of(...))
→ Files.exists(nested) == true
```

**#7 `writeAll_doubleEdgeValues_noLoss`**
```
Sample s0 = new Sample("S001", "zero", 0, 0.0, 0)
Sample s1 = new Sample("S002", "one",  0, 1.0, 0)
JsonFileUtil.writeAll(path, List.of(s0, s1))
List<Sample> loaded = JsonFileUtil.readAll(path, type)
→ assertEquals(0.0, loaded.get(0).getYield(), 0.0)
→ assertEquals(1.0, loaded.get(1).getYield(), 0.0)
// 0.0·1.0은 IEEE 754 이진 부동소수점으로 정확히 표현 가능 → delta=0.0 허용
// 0.85·0.92 등 비정밀 수율값은 delta=1e-9 사용
```

**#8~10 null 인자 테스트**
```
assertThrows(IllegalArgumentException.class,
    () -> JsonFileUtil.readAll(null, type))

assertThrows(IllegalArgumentException.class,
    () -> JsonFileUtil.writeAll(null, List.of()))

assertThrows(IllegalArgumentException.class,
    () -> JsonFileUtil.writeAll(path, null))
```

**#11 `readAll_blankFileContent_returnsEmptyList`**
```
Path path = tempDir.resolve("blank.json")
Files.writeString(path, "   \n  ")
List<Sample> result = JsonFileUtil.readAll(path, type)
→ result.isEmpty() == true
```

**#12 `readAll_emptyArrayContent_returnsEmptyList`**
```
Path path = tempDir.resolve("empty-array.json")
Files.writeString(path, "[]")
List<Sample> result = JsonFileUtil.readAll(path, type)
→ result.isEmpty() == true
```

**#13 `readAll_validJsonObject_returnsEmptyList`**
```
Path path = tempDir.resolve("object.json")
Files.writeString(path, "{\"sampleId\":\"S001\"}")
List<Sample> result = JsonFileUtil.readAll(path, type)
→ result.isEmpty() == true
// Gson이 배열 타입으로 단일 객체를 역직렬화하면 null을 반환 → 방어 처리
```

**#14 `writeAll_ioError_throwsUncheckedIOException`**
```
Path readOnlyPath = tempDir.resolve("readonly.json")
Files.writeString(readOnlyPath, "[]")
readOnlyPath.toFile().setReadOnly()  // 쓰기 거부
assertThrows(UncheckedIOException.class,
    () -> JsonFileUtil.writeAll(readOnlyPath, List.of(...)))
// Windows에서 setReadOnly()가 동작하지 않을 수 있으므로
// assumeTrue(readOnlyPath.toFile().canWrite() == false)로 조건부 실행
```

### 2.5 전체 코드 골격

```java
package org.ssemi.persistence.util;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.persistence.model.Sample;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileUtilTest {

    @TempDir
    Path tempDir;

    private static final Type SAMPLE_LIST_TYPE =
            new TypeToken<List<Sample>>() {}.getType();

    @Test
    void readAll_fileNotExists_returnsEmptyList() { ... }

    @Test
    void writeAll_thenReadAll_returnsSameData() { ... }

    @Test
    void writeAll_emptyList_writesEmptyArray() throws IOException { ... }

    @Test
    void writeAll_koreanString_utf8Preserved() { ... }

    @Test
    void readAll_corruptedJson_throwsException() throws IOException { ... }

    @Test
    void writeAll_dirNotExists_createsDir() { ... }

    @Test
    void writeAll_doubleEdgeValues_noLoss() { ... }

    @Test
    void readAll_nullPath_throwsException() { ... }

    @Test
    void writeAll_nullPath_throwsException() { ... }

    @Test
    void writeAll_nullItems_throwsException() { ... }
}
```

---

## 3. IOException 처리 전략

`JsonFileUtil`의 메서드 시그니처에 `throws IOException`을 선언하지 않는다.
Repository가 checked exception을 처리하는 부담 없이 호출할 수 있도록
`IOException`은 `UncheckedIOException`으로 래핑해 전파한다.

| 예외 | 처리 방식 |
|------|-----------|
| `IOException` | `UncheckedIOException`으로 래핑 후 전파 |
| `JsonSyntaxException` | 그대로 전파 (RuntimeException) |
| `IllegalArgumentException` | 직접 발생 (null 인자) |

---

## 4. Phase 3·4 연계 계약

이 유틸이 다음 불변식을 보장하면 Phase 3·4는 파일 I/O를 신뢰하고 비즈니스 로직에만 집중할 수 있다.

| 불변식 | 보장 방법 |
|--------|-----------|
| 파일 없을 때 `readAll`은 빈 리스트 반환 (예외 없음) | 테스트 #1 |
| `writeAll` 후 `readAll`은 동일 데이터 반환 | 테스트 #2 |
| `writeAll`은 디렉토리를 자동 생성 | 테스트 #6 |
| UTF-8 인코딩 보장 | 테스트 #4 |
| `double` 정밀도 보장 | 테스트 #7 |
| `IOException` 발생 시 `UncheckedIOException`으로 전파 (Repository는 checked 예외 선언 불필요) | 테스트 #14 |
| 배열이 아닌 JSON 내용은 빈 리스트로 안전하게 처리 | 테스트 #13 |

---

## 5. 완료 조건

- [ ] `JsonFileUtil.java` 생성
- [ ] `JsonFileUtilTest.java` 생성 — 14개 테스트 모두 통과
- [ ] `./gradlew test` — 기존 Phase 1 테스트(20개) + Phase 2 테스트(14개) 전체 통과
- [ ] `util` 패키지 라인 커버리지 90% 이상 (`./gradlew jacocoTestReport`)

---

## 6. 다음 Phase 연계

Phase 3(`JsonSampleRepository`)은 이 Phase에서 완성된 `JsonFileUtil`을 직접 호출한다.
Phase 2 완료 조건이 모두 충족된 후 Phase 3을 시작한다.
