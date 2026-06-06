package com.femfit.model;

/**
 * User roles in the FemFit system.
 */
public enum Role {
    CLIENT,
    TRAINER,
    ADMIN;

    /**
     * Maps numeric role_id from the database to a Role enum value.
     * 1 = CLIENT, 2 = TRAINER, 3 = ADMIN
     */
    public static Role fromId(int id) {
        return switch (id) {
            case 1 -> CLIENT;
            case 2 -> TRAINER;
            case 3 -> ADMIN;
            default -> throw new IllegalArgumentException("Unknown role_id: " + id);
        };
    }
}