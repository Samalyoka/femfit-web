package com.femfit.dao;

import com.femfit.model.TrainingCycle;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for TrainingCycle operations.
 * Maps to the {@code training_cycles} table.
 */
public interface TrainingCycleDao {

    /**
     * Returns all training cycles (active and inactive).
     * Used by admin to manage all cycles.
     *
     * @return list of all cycles ordered by created_at DESC
     */
    List<TrainingCycle> findAll();

    /**
     * Returns only active training cycles.
     * Used by clients to browse available programmes.
     *
     * @return list of active cycles
     */
    List<TrainingCycle> findAllActive();

    /**
     * Returns a single page of active training cycles, most recent first.
     * Used by clients to browse available programmes with pagination.
     *
     * @param offset SQL offset
     * @param limit  page size
     * @return list of active cycles for this page
     */
    List<TrainingCycle> findAllActive(int offset, int limit);

    /**
     * Finds a training cycle by ID.
     *
     * @param id the cycle ID
     * @return Optional containing the cycle if found
     */
    Optional<TrainingCycle> findById(Integer id);

    /**
     * Creates a new training cycle.
     *
     * @param cycle the cycle to create (id will be set from DB)
     * @return the saved cycle with generated id
     */
    TrainingCycle save(TrainingCycle cycle);

    /**
     * Updates an existing training cycle.
     *
     * @param cycle the cycle with updated fields
     */
    void update(TrainingCycle cycle);

    /**
     * Sets a cycle's active status.
     * Used to activate or deactivate a cycle.
     *
     * @param id     the cycle ID
     * @param active true to activate, false to deactivate
     */
    void setActive(Integer id, boolean active);

    /**
     * Counts total number of active cycles.
     * Used for admin statistics.
     *
     * @return count of active cycles
     */
    int countActive();

    /**
     * Counts total number of cycles (including inactive).
     *
     * @return total count
     */
    int countAll();
}