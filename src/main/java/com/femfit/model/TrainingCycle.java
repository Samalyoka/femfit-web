package com.femfit.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a training program available for purchase.
 * Maps to the {@code training_cycles} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingCycle {

    private Integer id;
    private String title;
    private String description;
    private Integer durationWeeks;
    private BigDecimal price;
    private boolean active;
    private LocalDateTime createdAt;
    private String photoUrl;
    private String titleRu;
    private String titleKz;
    private String descriptionRu;
    private String descriptionKz;

    /**
     * Returns the title in the given language, falling back to the
     * English title if no translation is set for that language.
     *
     * @param lang locale language code ("ru", "kz", or anything else for English)
     * @return localized title, never null (falls back to title)
     */
    public String getLocalizedTitle(String lang) {
        if ("ru".equals(lang) && titleRu != null && !titleRu.isBlank()) return titleRu;
        if ("kz".equals(lang) && titleKz != null && !titleKz.isBlank()) return titleKz;
        return title;
    }

    /**
     * Returns the description in the given language, falling back to the
     * English description if no translation is set for that language.
     *
     * @param lang locale language code ("ru", "kz", or anything else for English)
     * @return localized description, never null (falls back to description)
     */
    public String getLocalizedDescription(String lang) {
        if ("ru".equals(lang) && descriptionRu != null && !descriptionRu.isBlank()) return descriptionRu;
        if ("kz".equals(lang) && descriptionKz != null && !descriptionKz.isBlank()) return descriptionKz;
        return description;
    }
}