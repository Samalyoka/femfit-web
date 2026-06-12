package com.femfit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form payload for changing a member's password from the profile page.
 * Server-side validation ensures the new password meets minimum length
 * requirements; matching of newPassword/confirmPassword is checked
 * separately in the controller.
 */
public class ChangePasswordDto {

    @NotBlank(message = "{password.current.required}")
    private String currentPassword;

    @NotBlank(message = "{password.new.required}")
    @Size(min = 8, message = "{password.size}")
    private String newPassword;

    @NotBlank(message = "{password.confirm.required}")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}