package com.femfit.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for creating or editing a training cycle via the admin form.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingCycleDto {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be under 100 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must be under 2000 characters")
    private String description;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 week")
    @Max(value = 52, message = "Duration must be under 52 weeks")
    private Integer durationWeeks;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be positive")
    private BigDecimal price;
}