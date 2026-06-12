package org.ssemi.persistence.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void allArgsConstructor_fieldsSetCorrectly() {
        Order order = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED);

        assertEquals("O001", order.getOrderId());
        assertEquals("S001", order.getSampleId());
        assertEquals("홍길동", order.getCustomerName());
        assertEquals(10, order.getQuantity());
        assertEquals(OrderStatus.RESERVED, order.getStatus());
    }

    @Test
    void setters_updateFields() {
        Order order = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED);

        order.setOrderId("O002");
        order.setSampleId("S002");
        order.setCustomerName("김철수");
        order.setQuantity(20);
        order.setStatus(OrderStatus.CONFIRMED);

        assertEquals("O002", order.getOrderId());
        assertEquals("S002", order.getSampleId());
        assertEquals("김철수", order.getCustomerName());
        assertEquals(20, order.getQuantity());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void equals_sameOrderId_returnsTrue() {
        Order a = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED);
        Order b = new Order("O001", "S002", "김철수", 20, OrderStatus.CONFIRMED);

        assertEquals(a, b);
    }

    @Test
    void equals_differentOrderId_returnsFalse() {
        Order a = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED);
        Order b = new Order("O002", "S001", "홍길동", 10, OrderStatus.RESERVED);

        assertNotEquals(a, b);
    }

    @Test
    void equals_nullOrderId_bothNull_returnsTrue() {
        Order a = new Order(null, "S001", "홍길동", 10, OrderStatus.RESERVED);
        Order b = new Order(null, "S002", "김철수", 20, OrderStatus.CONFIRMED);

        assertEquals(a, b);
    }

    @Test
    void equals_nullOrderId_oneNull_returnsFalse() {
        Order a = new Order(null, "S001", "홍길동", 10, OrderStatus.RESERVED);
        Order b = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED);

        assertNotEquals(a, b);
    }

    @Test
    void equals_null_returnsFalse() {
        Order order = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED);

        assertNotEquals(null, order);
    }

    @Test
    void equals_differentType_returnsFalse() {
        Order order = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED);

        assertNotEquals("O001", order);
    }

    @Test
    void hashCode_sameOrderId_sameHash() {
        Order a = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED);
        Order b = new Order("O001", "S002", "김철수", 20, OrderStatus.CONFIRMED);

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsOrderIdAndStatus() {
        Order order = new Order("O001", "S001", "홍길동", 10, OrderStatus.RESERVED);
        String result = order.toString();

        assertTrue(result.contains("O001"));
        assertTrue(result.contains("RESERVED"));
    }
}
