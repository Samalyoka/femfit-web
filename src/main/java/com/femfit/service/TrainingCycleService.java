package com.femfit.service;

import com.femfit.model.TrainingCycle;
import java.util.List;
import java.util.Optional;

/**
 * Service for training cycle operations.
 */
public interface TrainingCycleService {

    /**
     * Returns all active training cycles available for purchase.
     */
    List<TrainingCycle> findAllActive();

    /**
     * Finds a training cycle by id.
     */
    Optional<TrainingCycle> findById(Integer id);
}