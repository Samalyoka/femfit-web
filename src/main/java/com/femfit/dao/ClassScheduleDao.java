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
     * Returns a paginated page of upcoming schedules (no category filter).
     * Enables the schedule page to scale as the class roster grows.
     *
     * @param offset SQL OFFSET (rows to skip)
     * @param limit  SQL LIMIT (rows to return)
     * @return list of upcoming schedules for this page, possibly empty
     */
    List<ClassSchedule> findUpcoming(int offset, int limit);

    /**
     * Returns the total count of upcoming schedules (no category filter).
     * Used together with {@link #findUpcoming(int, int)} to compute totalPages.
     *
     * @return total number of upcoming schedules
     */
    int countUpcoming();

    /**
     * Returns upcoming schedules filtered by category.
     *
     * @param category e.g. YOGA, CARDIO, STRENGTH
     * @return filtered list
     */
    List<ClassSchedule> findUpcomingByCategory(String category);

    /**
     * Returns a paginated page of upcoming schedules filtered by category.
     *
     * @param category e.g. YOGA, CARDIO, STRENGTH
     * @param offset   SQL OFFSET (rows to skip)
     * @param limit    SQL LIMIT (rows to return)
     * @return filtered and paginated list, possibly empty
     */
    List<ClassSchedule> findUpcomingByCategory(String category, int offset, int limit);

    /**
     * Returns the total count of upcoming schedules for a given category.
     * Used with {@link #findUpcomingByCategory(String, int, int)} to compute totalPages.
     *
     * @param category e.g. YOGA, CARDIO, STRENGTH
     * @return total count for the category
     */
    int countUpcomingByCategory(String category);

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