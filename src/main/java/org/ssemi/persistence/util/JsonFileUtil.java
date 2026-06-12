package org.ssemi.persistence.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class JsonFileUtil {

    // data/*.json을 사람이 직접 읽고 확인할 수 있도록 pretty print 적용 (PRD 섹션 4)
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
            // Gson이 배열 타입으로 객체·스칼라 JSON을 파싱할 때 IllegalStateException을 래핑해 던짐
            // → 구조 불일치(배열 아닌 형태)이므로 빈 리스트로 안전하게 처리
            if (e.getCause() instanceof IllegalStateException) return Collections.emptyList();
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
