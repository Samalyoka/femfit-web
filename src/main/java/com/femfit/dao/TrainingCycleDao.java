package com.femfit.dao;

import com.femfit.model.TrainingCycle;
import java.util.List;
import java.util.Optional;

public interface TrainingCycleDao {
    List<TrainingCycle> findAllActive();
    Optional<TrainingCycle> findById(Integer id);
}