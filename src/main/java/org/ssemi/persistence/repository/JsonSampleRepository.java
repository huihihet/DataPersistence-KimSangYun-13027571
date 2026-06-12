package org.ssemi.persistence.repository;

import com.google.gson.reflect.TypeToken;
import org.ssemi.persistence.model.Sample;
import org.ssemi.persistence.util.JsonFileUtil;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class JsonSampleRepository implements SampleRepository {

    private static final Type SAMPLE_LIST_TYPE =
            new TypeToken<List<Sample>>() {}.getType();

    private final Path filePath;

    // 테스트 시 @TempDir 경로를 주입해 파일 격리 가능
    public JsonSampleRepository(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(Sample sample) {
        if (sample == null) throw new IllegalArgumentException("sample must not be null");
        List<Sample> list = loadMutable();
        if (list.stream().anyMatch(s -> s.getSampleId().equals(sample.getSampleId()))) {
            throw new IllegalArgumentException("Duplicate sampleId: " + sample.getSampleId());
        }
        list.add(sample);
        JsonFileUtil.writeAll(filePath, list);
    }

    @Override
    public Optional<Sample> findById(String sampleId) {
        if (sampleId == null) throw new IllegalArgumentException("sampleId must not be null");
        List<Sample> all = JsonFileUtil.readAll(filePath, SAMPLE_LIST_TYPE);
        return all.stream()
                .filter(s -> s.getSampleId().equals(sampleId))
                .findFirst();
    }

    @Override
    public List<Sample> findAll() {
        return JsonFileUtil.readAll(filePath, SAMPLE_LIST_TYPE);
    }

    @Override
    public void update(Sample sample) {
        if (sample == null) throw new IllegalArgumentException("sample must not be null");
        List<Sample> list = loadMutable();
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getSampleId().equals(sample.getSampleId())) {
                idx = i;
                break;
            }
        }
        if (idx == -1) {
            throw new NoSuchElementException("sampleId not found: " + sample.getSampleId());
        }
        list.set(idx, sample);
        JsonFileUtil.writeAll(filePath, list);
    }

    @Override
    public void deleteById(String sampleId) {
        if (sampleId == null) throw new IllegalArgumentException("sampleId must not be null");
        List<Sample> list = loadMutable();
        boolean removed = list.removeIf(s -> s.getSampleId().equals(sampleId));
        if (!removed) {
            throw new NoSuchElementException("sampleId not found: " + sampleId);
        }
        JsonFileUtil.writeAll(filePath, list);
    }

    private List<Sample> loadMutable() {
        return new ArrayList<>(JsonFileUtil.readAll(filePath, SAMPLE_LIST_TYPE));
    }
}
