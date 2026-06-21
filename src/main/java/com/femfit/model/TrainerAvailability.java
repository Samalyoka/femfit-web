package com.femfit.model;

/**
 * A trainer's current availability for new assignments.
 *
 * <p>Distinct from {@code members.is_active} (whether the trainer's
 * account is enabled in the system at all). A trainer can be an active
 * member but temporarily UNAVAILABLE (e.g. on vacation or sick leave),
 * in which case an admin may reassign their orders to an AVAILABLE
 * trainer — see {@link com.femfit.service.OrderService#assignTrainer}.</p>
 */
public enum TrainerAvailability {
    AVAILABLE,
    UNAVAILABLE
}