package org.ssemi.persistence.repository;

import org.ssemi.persistence.model.Order;
import org.ssemi.persistence.model.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String orderId);
    List<Order> findAll();
    List<Order> findByStatus(OrderStatus status);
    void update(Order order);
    void deleteById(String orderId);
}
