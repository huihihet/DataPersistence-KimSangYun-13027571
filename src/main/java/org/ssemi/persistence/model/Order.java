package org.ssemi.persistence.model;

import java.util.Objects;

public class Order {

    private String orderId;
    private String sampleId;
    private String customerName;
    private int quantity;
    private OrderStatus status;

    // Gson 역직렬화용 — 리플렉션 접근 가능하므로 public 불필요
    Order() {}

    public Order(String orderId, String sampleId, String customerName, int quantity, OrderStatus status) {
        this.orderId = orderId;
        this.sampleId = sampleId;
        this.customerName = customerName;
        this.quantity = quantity;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getSampleId() { return sampleId; }
    public void setSampleId(String sampleId) { this.sampleId = sampleId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return "Order{orderId='" + orderId + "', sampleId='" + sampleId + "', status=" + status + "}";
    }
}
