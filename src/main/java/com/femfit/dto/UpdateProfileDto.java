package com.femfit.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

/**
 * DTO for editing a member's own profile (firstName, lastName, phone, birthDate).
 * Email and password are not editable here — email is the login identifier,
 * password is changed via a separate {@link ChangePasswordDto} form.
 * Validated with Bean Validation (jakarta.validation).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileDto {

    @NotBlank(message = "{error.required}")
    @Size(min = 2, max = 100)
    @Pattern(regexp = "^[\\p{L} '-]+$", message = "{error.name.invalid}")
    private String firstName;

    @NotBlank(message = "{error.required}")
    @Size(min = 2, max = 100)
    @Pattern(regexp = "^[\\p{L} '-]+$", message = "{error.name.invalid}")
    private String lastName;

    // Allowed to be null or empty
    @Pattern(regexp = "^(\\+?[0-9]{10,15})?$", message = "Invalid phone format")
    private String phone;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
}