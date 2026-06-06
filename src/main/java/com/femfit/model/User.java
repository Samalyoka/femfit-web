package com.femfit.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * Represents a system user (Client, Trainer, or Admin).
 * Maps to the {@code users} table.
 *
 * <p>Key entity — identified by {@code id} (primary key).</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String passwordHash;
    private LocalDate birthDate;

    private Role role;
    private boolean active;
    private LocalDateTime registrationDate;
    private int discountPercent;

    /**
     * Derived attribute — calculated from birthDate.
     *
     * @return age in full years, or -1 if birthDate is null
     */
    public int getAge() {
        if (birthDate == null) return -1;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Returns full name (first + last).
     *
     * @return concatenated full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Returns initials for avatar display.
     *
     * @return two-letter initials, e.g. "AK"
     */
    public String getInitials() {
        String f = (firstName != null && !firstName.isEmpty()) ? String.valueOf(firstName.charAt(0)) : "";
        String l = (lastName != null && !lastName.isEmpty()) ? String.valueOf(lastName.charAt(0)) : "";
        return (f + l).toUpperCase();
    }

    public void setEnabled(boolean enabled) {
    }
}