package com.femfit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Projection for trainer selection (e.g. choose-trainer page).
 * Exposes only the fields a client needs to see when picking a trainer —
 * does not leak password hashes, role, discount, or other Member-only fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerDto {

    private long id;
    private String firstName;
    private String lastName;
    private String email;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}