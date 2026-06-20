package com.femfit.dao;

import com.femfit.dto.ClientOrderDto;
import com.femfit.dto.TrainerDto;
import com.femfit.dto.TrainerProfileDto;
import com.femfit.model.Member;

import java.util.List;

/**
 * DAO interface for trainer-related queries.
 *
 * <p>Trainers are members with role {@code TRAINER} that additionally have
 * a row in the {@code trainers} table. This DAO provides queries that join
 * across {@code members}, {@code trainers} and {@code orders} to support
 * the trainer dashboard and trainer-selection UI.</p>
 */
public interface TrainerDao {

    /**
     * Resolves the {@code trainers.id} for a given member id.
     *
     * @param userId the member id of the trainer (members.id)
     * @return the trainer id (trainers.id, same value as members.id)
     * @throws RuntimeException if no trainer record exists for the given member id
     */
    long findTrainerIdByUserId(long userId);

    /**
     * Returns all clients (members) who currently have an active or
     * in-progress order assigned to the given trainer.
     *
     * @param trainerId the trainer's id
     * @return list of clients, possibly empty
     */
    List<Member> findClientsByTrainerId(long trainerId);

    /**
     * Returns a lightweight projection of all active trainers
     * (id, name, email) for trainer-selection UI.
     *
     * @return list of active trainers as {@link TrainerDto}, possibly empty
     */
    List<TrainerDto> findAllTrainers();

    /**
     * Returns all active trainers enriched with their average review rating
     * and review count, computed via LEFT JOIN across orders → reviews.
     * Trainers with no reviews yet have averageRating = null, reviewCount = 0.
     * Used on the choose-trainer page so clients can pick by rating.
     *
     * @return list of active trainers with rating info, possibly empty
     */
    List<TrainerDto> findAllTrainersWithRating();

    /**
     * Get last active order for each client of the trainer.
     * One row per client, with order details if exists, otherwise nulls for order fields.
     *
     * @param trainerId the trainer's id
     * @return list of client/order projections, possibly empty
     */
    List<ClientOrderDto> findClientsWithOrderByTrainerId(long trainerId);

    /**
     * Returns full public profiles for all active trainers — photo, specialization,
     * bio, certification, experience years, and average review rating — for the
     * public-facing "Our Trainers" page (about/trainers.html). Unlike
     * {@link #findAllTrainersWithRating()}, this includes the marketing/profile
     * fields needed to render a full trainer card, not just the lightweight
     * picker fields.
     *
     * @return list of trainer profiles, possibly empty
     */
    List<TrainerProfileDto> findAllTrainerProfiles();
}