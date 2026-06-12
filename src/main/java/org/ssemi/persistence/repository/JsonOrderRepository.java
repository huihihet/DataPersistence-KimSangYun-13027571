package org.ssemi.persistence.repository;

import com.google.gson.reflect.TypeToken;
import org.ssemi.persistence.model.Order;
import org.ssemi.persistence.model.OrderStatus;
import org.ssemi.persistence.util.JsonFileUtil;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonOrderRepository implements OrderRepository {

    private static final Type ORDER_LIST_TYPE =
            new TypeToken<List<Order>>() {}.getType();

    private final Path filePath;

    // 테스트 시 @TempDir 경로를 주입해 파일 격리 가능
    public JsonOrderRepository(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(Order order) {
        if (order == null) throw new IllegalArgumentException("order must not be null");
        List<Order> list = loadMutable();
        if (list.stream().anyMatch(o -> o.getOrderId().equals(order.getOrderId()))) {
            throw new IllegalArgumentException("Duplicate orderId: " + order.getOrderId());
        }
        list.add(order);
        JsonFileUtil.writeAll(filePath, list);
    }

    @Override
    public Optional<Order> findById(String orderId) {
        if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
        List<Order> all = JsonFileUtil.readAll(filePath, ORDER_LIST_TYPE);
        return all.stream()
                .filter(o -> o.getOrderId().equals(orderId))
                .findFirst();
    }

    @Override
    public List<Order> findAll() {
        return JsonFileUtil.readAll(filePath, ORDER_LIST_TYPE);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        if (status == null) throw new IllegalArgumentException("status must not be null");
        return findAll().stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public void update(Order order) {
        if (order == null) throw new IllegalArgumentException("order must not be null");
        List<Order> list = loadMutable();
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getOrderId().equals(order.getOrderId())) {
                idx = i;
                break;
            }
        }
        if (idx == -1) {
            throw new NoSuchElementException("orderId not found: " + order.getOrderId());
        }
        list.set(idx, order);
        JsonFileUtil.writeAll(filePath, list);
    }

    @Override
    public void deleteById(String orderId) {
        if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
        List<Order> list = loadMutable();
        boolean removed = list.removeIf(o -> o.getOrderId().equals(orderId));
        if (!removed) {
            throw new NoSuchElementException("orderId not found: " + orderId);
        }
        JsonFileUtil.writeAll(filePath, list);
    }

    private List<Order> loadMutable() {
        return new ArrayList<>(JsonFileUtil.readAll(filePath, ORDER_LIST_TYPE));
    }
}
