package com.femfit.dto;

import com.femfit.model.Assignment;
import com.femfit.model.Order;
import com.femfit.model.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single entry in a client's archive of completed training programmes —
 * pairs a COMPLETED {@link Order} with its final {@link Assignment} (the
 * trainer's last set of exercises/equipment/nutrition/schedule, kept as a
 * record of what the client followed) and {@link Review}, if the client
 * left one. Used by the "My Archive" page (client/archive.html).
 *
 * @see com.femfit.service.OrderService#getArchive
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchiveEntryDto {

    private Order order;

    /** The final assignment for this order, if one was ever created. */
    private Assignment assignment;

    /** The client's review for this order, if they left one. */
    private Review review;

    /** True if the client has not yet reviewed this completed programme. */
    public boolean isUnreviewed() {
        return review == null;
    }
}