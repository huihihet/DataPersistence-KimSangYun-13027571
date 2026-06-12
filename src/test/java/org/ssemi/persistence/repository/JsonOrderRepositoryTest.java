package org.ssemi.persistence.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.persistence.model.Order;
import org.ssemi.persistence.model.OrderStatus;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class JsonOrderRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonOrderRepository repo;

    @BeforeEach
    void setUp() {
        repo = new JsonOrderRepository(tempDir.resolve("orders.json"));
    }

    @Test
    void save_thenFindById_returnsOrder() {
        Order o = new Order("O001", "S001", "서울대", 10, OrderStatus.RESERVED);

        repo.save(o);

        assertEquals(o, repo.findById("O001").orElseThrow());
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
    void findByStatus_returnsFilteredList() {
        repo.save(new Order("O001", "S001", "서울대", 10, OrderStatus.RESERVED));
        repo.save(new Order("O002", "S001", "카이스트", 5, OrderStatus.PRODUCING));
        repo.save(new Order("O003", "S002", "연세대", 3, OrderStatus.RESERVED));

        List<Order> result = repo.findByStatus(OrderStatus.RESERVED);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(o -> o.getOrderId().equals("O001")));
        assertTrue(result.stream().anyMatch(o -> o.getOrderId().equals("O003")));
    }

    @Test
    void findByStatus_noMatch_returnsEmpty() {
        repo.save(new Order("O001", "S001", "서울대", 10, OrderStatus.RESERVED));

        List<Order> result = repo.findByStatus(OrderStatus.RELEASE);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByStatus_null_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repo.findByStatus(null));
    }

    @Test
    void update_status_changesReflected() {
        repo.save(new Order("O001", "S001", "서울대", 10, OrderStatus.RESERVED));

        Order updated = new Order("O001", "S001", "서울대", 10, OrderStatus.CONFIRMED);
        repo.update(updated);

        assertEquals(OrderStatus.CONFIRMED, repo.findById("O001").get().getStatus());
    }

    @Test
    void update_notExists_throwsException() {
        assertThrows(NoSuchElementException.class,
                () -> repo.update(new Order("NONE", "S001", "없는고객", 1, OrderStatus.RESERVED)));
    }

    @Test
    void deleteById_existing_removedFromList() {
        repo.save(new Order("O001", "S001", "서울대", 10, OrderStatus.RESERVED));

        repo.deleteById("O001");

        assertTrue(repo.findById("O001").isEmpty());
    }

    @Test
    void deleteById_notExists_throwsException() {
        assertThrows(NoSuchElementException.class,
                () -> repo.deleteById("NONE"));
    }

    @Test
    void save_duplicateId_throwsException() {
        repo.save(new Order("O001", "S001", "서울대", 10, OrderStatus.RESERVED));

        assertThrows(IllegalArgumentException.class,
                () -> repo.save(new Order("O001", "S002", "카이스트", 5, OrderStatus.PRODUCING)));
    }

    @Test
    void persistence_afterReopen_dataPreserved() {
        Path path = tempDir.resolve("orders.json");
        new JsonOrderRepository(path).save(new Order("O001", "S001", "서울대", 10, OrderStatus.RESERVED));

        JsonOrderRepository repo2 = new JsonOrderRepository(path);

        assertEquals(1, repo2.findAll().size());
        assertTrue(repo2.findById("O001").isPresent());
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
