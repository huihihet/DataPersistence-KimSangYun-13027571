package org.ssemi.persistence.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.persistence.model.Sample;

import java.nio.file.Path;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class JsonSampleRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonSampleRepository repo;

    @BeforeEach
    void setUp() {
        repo = new JsonSampleRepository(tempDir.resolve("samples.json"));
    }

    @Test
    void save_thenFindById_returnsSample() {
        Sample s = new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 50);

        repo.save(s);

        assertEquals(s, repo.findById("S001").orElseThrow());
    }

    @Test
    void save_duplicateId_throwsException() {
        Sample s = new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 50);
        repo.save(s);

        assertThrows(IllegalArgumentException.class,
                () -> repo.save(new Sample("S001", "다른 이름", 60, 0.9, 10)));
    }

    @Test
    void findAll_initiallyEmpty() {
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void findById_notExists_returnsEmpty() {
        assertTrue(repo.findById("X").isEmpty());
    }

    @Test
    void update_existing_fieldsChanged() {
        repo.save(new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 50));

        repo.update(new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 999));
        Sample found = repo.findById("S001").orElseThrow();

        assertEquals(999, found.getStock());
    }

    @Test
    void update_notExists_throwsException() {
        assertThrows(NoSuchElementException.class,
                () -> repo.update(new Sample("NONE", "없는 시료", 0, 0.0, 0)));
    }

    @Test
    void deleteById_existing_removedFromList() {
        repo.save(new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 50));

        repo.deleteById("S001");

        assertTrue(repo.findById("S001").isEmpty());
    }

    @Test
    void deleteById_notExists_throwsException() {
        assertThrows(NoSuchElementException.class,
                () -> repo.deleteById("NONE"));
    }

    @Test
    void persistence_afterReopen_dataPreserved() {
        Path path = tempDir.resolve("samples.json");
        new JsonSampleRepository(path).save(new Sample("S001", "GaN 웨이퍼 A", 120, 0.85, 50));

        JsonSampleRepository repo2 = new JsonSampleRepository(path);

        assertEquals(1, repo2.findAll().size());
        assertTrue(repo2.findById("S001").isPresent());
    }

    @Test
    void save_null_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repo.save(null));
    }

    @Test
    void findById_null_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repo.findById(null));
    }

    @Test
    void deleteById_null_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repo.deleteById(null));
    }

    @Test
    void update_null_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repo.update(null));
    }
}
