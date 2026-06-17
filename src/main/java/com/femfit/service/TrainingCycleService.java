package com.femfit.service;

import com.femfit.dto.TrainingCycleDto;
import com.femfit.model.TrainingCycle;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing training cycles.
 * Used by admin (full CRUD) and clients (browse active).
 */
public interface TrainingCycleService {

    /** All cycles — for admin management page. */
    List<TrainingCycle> findAll();

    /** Active cycles only — for client browse page. */
    List<TrainingCycle> findAllActive();

    /** Find by ID. */
    Optional<TrainingCycle> findById(Integer id);

    /** Create a new cycle from admin form data. */
    TrainingCycle create(TrainingCycleDto dto);

    /** Update an existing cycle from admin form data. */
    void update(Integer id, TrainingCycleDto dto);

    /** Deactivate a cycle (soft delete). */
    void deactivate(Integer id);

    /** Activate a previously deactivated cycle. */
    void activate(Integer id);

    /** Count of active cycles for statistics. */
    int countActive();
}

