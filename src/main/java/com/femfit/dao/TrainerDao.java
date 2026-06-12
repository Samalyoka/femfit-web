package com.femfit.dao;

import com.femfit.dto.ClientOrderDto;
import com.femfit.model.Member;

import java.util.List;

public interface TrainerDao {
    long findTrainerIdByUserId(long userId);
    List<Member> findClientsByTrainerId(long trainerId);
    List<Member> findAllTrainers();

    /**
     * Get last active order for each client of the trainer.
     * One row per client, with order details if exists, otherwise nulls for order fields.
     */
    List<ClientOrderDto> findClientsWithOrderByTrainerId(long trainerId);
}