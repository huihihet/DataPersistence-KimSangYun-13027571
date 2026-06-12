package org.ssemi.persistence.model;

import java.util.Objects;

public class Sample {

    private String sampleId;
    private String name;
    private int avgProductionTime;
    private double yield;
    private int stock;

    // Gson 역직렬화용 — 리플렉션 접근 가능하므로 public 불필요
    Sample() {}

    public Sample(String sampleId, String name, int avgProductionTime, double yield, int stock) {
        this.sampleId = sampleId;
        this.name = name;
        this.avgProductionTime = avgProductionTime;
        this.yield = yield;
        this.stock = stock;
    }

    public String getSampleId() { return sampleId; }
    public void setSampleId(String sampleId) { this.sampleId = sampleId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAvgProductionTime() { return avgProductionTime; }
    public void setAvgProductionTime(int avgProductionTime) { this.avgProductionTime = avgProductionTime; }

    public double getYield() { return yield; }
    public void setYield(double yield) { this.yield = yield; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sample sample = (Sample) o;
        return Objects.equals(sampleId, sample.sampleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sampleId);
    }

    @Override
    public String toString() {
        return "Sample{sampleId='" + sampleId + "', name='" + name + "', stock=" + stock + "}";
    }
}
