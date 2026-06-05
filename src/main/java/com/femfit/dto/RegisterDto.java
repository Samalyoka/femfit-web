package com.femfit.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * DTO for user registration form.
 * Validated with Bean Validation (jakarta.validation).
 */
@Data
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

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone format")
    private String phone;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
}