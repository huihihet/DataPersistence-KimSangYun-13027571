package org.ssemi.persistence.repository;

import org.ssemi.persistence.model.Sample;

import java.util.List;
import java.util.Optional;

public interface SampleRepository {
    void save(Sample sample);
    Optional<Sample> findById(String sampleId);
    List<Sample> findAll();
    void update(Sample sample);
    void deleteById(String sampleId);
}
