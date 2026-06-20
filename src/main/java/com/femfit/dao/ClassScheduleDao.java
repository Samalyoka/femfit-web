package com.femfit.dao;

import com.femfit.model.ClassSchedule;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for {@link ClassSchedule} entity.
 */
public interface ClassScheduleDao {

    /**
     * Finds a schedule by id with joined class and trainer info.
     *
     * @param id schedule id
     * @return Optional with schedule or empty
     */
    Optional<ClassSchedule> findById(Long id);

    /**
     * Returns upcoming schedules for the next 7 days with spots left.
     *
     * @return list of upcoming class schedules
     */
    List<ClassSchedule> findUpcoming();

    /**
     * Returns upcoming schedules filtered by category.
     *
     * @param category e.g. YOGA, CARDIO, STRENGTH
     * @return filtered list
     */
    List<ClassSchedule> findUpcomingByCategory(String category);

    /**
     * Generates dated, bookable occurrences for every active recurring
     * schedule template, covering the next {@code weeksAhead} weeks from
     * today. Idempotent — safe to call repeatedly (e.g. on every app
     * startup). This is what keeps the public schedule page from ever
     * running out of upcoming classes to show.
     *
     * @param weeksAhead how many weeks ahead to ensure occurrences exist for
     */
    void generateUpcomingOccurrences(int weeksAhead);
}