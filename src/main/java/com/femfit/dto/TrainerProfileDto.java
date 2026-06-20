package com.femfit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public-facing trainer profile for the "Our Trainers" page (about/trainers.html).
 * Unlike {@link TrainerDto} (used on the lightweight choose-trainer picker),
 * this projection includes the marketing/profile fields — photo, specialization,
 * bio, certification and years of experience — alongside the same rating
 * fields, so the page can show a full card per trainer without a second query.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerProfileDto {

    private long id;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private String specialization;
    private String bio;
    private int experienceYears;
    private String certification;
    private String specializationRu;
    private String specializationKz;
    private String bioRu;
    private String bioKz;

    /**
     * Average review rating (1-5) across all this trainer's completed orders.
     * Null if the trainer has no reviews yet.
     */
    private Double averageRating;

    /** Number of reviews behind the averageRating, for display ("4.8 (12 reviews)"). */
    private Integer reviewCount;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** True if this trainer has at least one review. */
    public boolean hasRating() {
        return averageRating != null && reviewCount != null && reviewCount > 0;
    }

    /**
     * Returns the specialization label in the given language, falling back
     * to the English specialization if no translation is set.
     *
     * @param lang locale language code ("ru", "kz", or anything else for English)
     * @return localized specialization, possibly null if none set in any language
     */
    public String getLocalizedSpecialization(String lang) {
        if ("ru".equals(lang) && specializationRu != null && !specializationRu.isBlank()) return specializationRu;
        if ("kz".equals(lang) && specializationKz != null && !specializationKz.isBlank()) return specializationKz;
        return specialization;
    }

    /**
     * Returns the bio in the given language, falling back to the English
     * bio if no translation is set.
     *
     * @param lang locale language code ("ru", "kz", or anything else for English)
     * @return localized bio, possibly null if none set in any language
     */
    public String getLocalizedBio(String lang) {
        if ("ru".equals(lang) && bioRu != null && !bioRu.isBlank()) return bioRu;
        if ("kz".equals(lang) && bioKz != null && !bioKz.isBlank()) return bioKz;
        return bio;
    }
}
