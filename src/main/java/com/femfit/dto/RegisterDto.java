package com.femfit.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

/**
 * DTO for user registration form.
 * Validated with Bean Validation (jakarta.validation).
 */
@Data
@NoArgsConstructor  // Ensures clean default initialization without pre-filled data
@AllArgsConstructor
public class RegisterDto {

    @NotBlank(message = "{error.required}")
    @Size(min = 2, max = 100)
    private String firstName;

    @NotBlank(message = "{error.required}")
    @Size(min = 2, max = 100)
    private String lastName;

    @NotBlank(message = "{error.required}")
    @Email(message = "{error.email.invalid}")
    private String email;

    @NotBlank(message = "{error.required}")
    @Size(min = 8, max = 255, message = "{error.password.short}")
    private String password;

    // Allowed phone to be null or empty during initial form binding
    @Pattern(regexp = "^(\\+?[0-9]{10,15})?$", message = "Invalid phone format")
    private String phone;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
}