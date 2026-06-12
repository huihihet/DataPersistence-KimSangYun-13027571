package org.ssemi.persistence.util;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.persistence.model.Sample;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class JsonFileUtilTest {

    @TempDir
    Path tempDir;

    private static final Type SAMPLE_LIST_TYPE =
            new TypeToken<List<Sample>>() {}.getType();

    @Test
    void readAll_fileNotExists_returnsEmptyList() {
        Path path = tempDir.resolve("nonexistent.json");

        List<Sample> result = JsonFileUtil.readAll(path, SAMPLE_LIST_TYPE);

        assertTrue(result.isEmpty());
    }

    @Test
    void writeAll_thenReadAll_returnsSameData() {
        Sample s = new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 50);
        Path path = tempDir.resolve("samples.json");

        JsonFileUtil.writeAll(path, List.of(s));
        List<Sample> loaded = JsonFileUtil.readAll(path, SAMPLE_LIST_TYPE);

        assertEquals(1, loaded.size());
        assertEquals(s, loaded.get(0));
        assertEquals(50, loaded.get(0).getStock());
    }

    @Test
    void writeAll_emptyList_writesEmptyArray() throws IOException {
        Path path = tempDir.resolve("empty.json");

        JsonFileUtil.writeAll(path, Collections.emptyList());
        String content = Files.readString(path, StandardCharsets.UTF_8);

        assertTrue(content.contains("[]"));
    }

    @Test
    void writeAll_koreanString_utf8Preserved() {
        Sample s = new Sample("S001", "GaN 웨이퍼", 0, 0.0, 0);
        Path path = tempDir.resolve("korean.json");

        JsonFileUtil.writeAll(path, List.of(s));
        List<Sample> loaded = JsonFileUtil.readAll(path, SAMPLE_LIST_TYPE);

        assertEquals("GaN 웨이퍼", loaded.get(0).getName());
    }

    @Test
    void readAll_corruptedJson_throwsException() throws IOException {
        Path path = tempDir.resolve("bad.json");
        // 배열로 시작하지만 내부가 손상된 JSON → Gson이 MalformedJsonException을 원인으로 JsonSyntaxException 전파
        Files.writeString(path, "[{not valid json", StandardCharsets.UTF_8);

        assertThrows(JsonSyntaxException.class,
                () -> JsonFileUtil.readAll(path, SAMPLE_LIST_TYPE));
    }

    @Test
    void writeAll_dirNotExists_createsDir() {
        Path nested = tempDir.resolve("a/b/c/samples.json");

        JsonFileUtil.writeAll(nested, List.of(new Sample("S001", "test", 0, 0.0, 0)));

        assertTrue(Files.exists(nested));
    }

    @Test
    void writeAll_doubleEdgeValues_noLoss() {
        Sample s0 = new Sample("S001", "zero", 0, 0.0, 0);
        Sample s1 = new Sample("S002", "one", 0, 1.0, 0);
        Path path = tempDir.resolve("doubles.json");

        JsonFileUtil.writeAll(path, List.of(s0, s1));
        List<Sample> loaded = JsonFileUtil.readAll(path, SAMPLE_LIST_TYPE);

        // 0.0·1.0은 IEEE 754 이진 부동소수점으로 정확히 표현 가능 → delta=0.0 허용
        assertEquals(0.0, loaded.get(0).getYield(), 0.0);
        assertEquals(1.0, loaded.get(1).getYield(), 0.0);
    }

    @Test
    void readAll_nullPath_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonFileUtil.readAll(null, SAMPLE_LIST_TYPE));
    }

    @Test
    void writeAll_nullPath_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonFileUtil.writeAll(null, List.of()));
    }

    @Test
    void writeAll_nullItems_throwsException() {
        Path path = tempDir.resolve("any.json");

        assertThrows(IllegalArgumentException.class,
                () -> JsonFileUtil.writeAll(path, null));
    }

    @Test
    void readAll_blankFileContent_returnsEmptyList() throws IOException {
        Path path = tempDir.resolve("blank.json");
        Files.writeString(path, "   \n  ", StandardCharsets.UTF_8);

        List<Sample> result = JsonFileUtil.readAll(path, SAMPLE_LIST_TYPE);

        assertTrue(result.isEmpty());
    }

    @Test
    void readAll_emptyArrayContent_returnsEmptyList() throws IOException {
        Path path = tempDir.resolve("empty-array.json");
        Files.writeString(path, "[]", StandardCharsets.UTF_8);

        List<Sample> result = JsonFileUtil.readAll(path, SAMPLE_LIST_TYPE);

        assertTrue(result.isEmpty());
    }

    @Test
    void readAll_validJsonObject_returnsEmptyList() throws IOException {
        Path path = tempDir.resolve("object.json");
        Files.writeString(path, "{\"sampleId\":\"S001\"}", StandardCharsets.UTF_8);

        List<Sample> result = JsonFileUtil.readAll(path, SAMPLE_LIST_TYPE);

        assertTrue(result.isEmpty());
    }

    @Test
    void writeAll_ioError_throwsUncheckedIOException() throws IOException {
        Path readOnlyPath = tempDir.resolve("readonly.json");
        Files.writeString(readOnlyPath, "[]", StandardCharsets.UTF_8);
        readOnlyPath.toFile().setReadOnly();

        // Windows에서 setReadOnly()가 관리자 권한 등에 따라 동작하지 않을 수 있으므로 조건부 실행
        assumeTrue(!readOnlyPath.toFile().canWrite(),
                "setReadOnly()가 이 환경에서 적용되지 않아 테스트 건너뜀");

        assertThrows(UncheckedIOException.class,
                () -> JsonFileUtil.writeAll(readOnlyPath,
                        List.of(new Sample("S001", "test", 0, 0.0, 0))));
    }
}
