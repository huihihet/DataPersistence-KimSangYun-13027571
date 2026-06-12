package org.ssemi.persistence.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SampleTest {

    @Test
    void allArgsConstructor_fieldsSetCorrectly() {
        Sample sample = new Sample("S001", "GaAs Wafer", 120, 0.95, 50);

        assertEquals("S001", sample.getSampleId());
        assertEquals("GaAs Wafer", sample.getName());
        assertEquals(120, sample.getAvgProductionTime());
        assertEquals(0.95, sample.getYield());
        assertEquals(50, sample.getStock());
    }

    @Test
    void setters_updateFields() {
        Sample sample = new Sample("S001", "GaAs Wafer", 120, 0.95, 50);

        sample.setSampleId("S002");
        sample.setName("InP Wafer");
        sample.setAvgProductionTime(200);
        sample.setYield(0.80);
        sample.setStock(30);

        assertEquals("S002", sample.getSampleId());
        assertEquals("InP Wafer", sample.getName());
        assertEquals(200, sample.getAvgProductionTime());
        assertEquals(0.80, sample.getYield());
        assertEquals(30, sample.getStock());
    }

    @Test
    void equals_sameSampleId_returnsTrue() {
        Sample a = new Sample("S001", "GaAs Wafer", 120, 0.95, 50);
        Sample b = new Sample("S001", "Different Name", 60, 0.70, 10);

        assertEquals(a, b);
    }

    @Test
    void equals_differentSampleId_returnsFalse() {
        Sample a = new Sample("S001", "GaAs Wafer", 120, 0.95, 50);
        Sample b = new Sample("S002", "GaAs Wafer", 120, 0.95, 50);

        assertNotEquals(a, b);
    }

    @Test
    void equals_nullSampleId_bothNull_returnsTrue() {
        Sample a = new Sample(null, "GaAs Wafer", 120, 0.95, 50);
        Sample b = new Sample(null, "InP Wafer", 60, 0.80, 20);

        assertEquals(a, b);
    }

    @Test
    void equals_nullSampleId_oneNull_returnsFalse() {
        Sample a = new Sample(null, "GaAs Wafer", 120, 0.95, 50);
        Sample b = new Sample("S001", "GaAs Wafer", 120, 0.95, 50);

        assertNotEquals(a, b);
    }

    @Test
    void equals_null_returnsFalse() {
        Sample sample = new Sample("S001", "GaAs Wafer", 120, 0.95, 50);

        assertNotEquals(null, sample);
    }

    @Test
    void equals_differentType_returnsFalse() {
        Sample sample = new Sample("S001", "GaAs Wafer", 120, 0.95, 50);

        assertNotEquals("S001", sample);
    }

    @Test
    void hashCode_sameSampleId_sameHash() {
        Sample a = new Sample("S001", "GaAs Wafer", 120, 0.95, 50);
        Sample b = new Sample("S001", "Different Name", 60, 0.70, 10);

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsSampleIdAndName() {
        Sample sample = new Sample("S001", "GaAs Wafer", 120, 0.95, 50);
        String result = sample.toString();

        assertTrue(result.contains("S001"));
        assertTrue(result.contains("GaAs Wafer"));
    }
}
