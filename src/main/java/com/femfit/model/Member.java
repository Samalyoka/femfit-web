package com.femfit.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * Represents a system member (Client, Trainer, or Admin).
 * Maps to the {@code members} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

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
    private AccountType accountType;

    /** Relative path to uploaded avatar, e.g. /static/img/avatars/42.jpg */
    private String avatarUrl;

    public int getAge() {
        if (birthDate == null) return -1;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getInitials() {
        String f = (firstName != null && !firstName.isEmpty()) ? String.valueOf(firstName.charAt(0)) : "";
        String l = (lastName != null && !lastName.isEmpty()) ? String.valueOf(lastName.charAt(0)) : "";
        return (f + l).toUpperCase();
    }

    public void setEnabled(boolean enabled) { this.active = enabled; }
    public boolean isEnabled() { return active; }
}
