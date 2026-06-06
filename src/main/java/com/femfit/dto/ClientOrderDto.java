package com.femfit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Projection for the trainer dashboard:
 * pairs a client with their active order id.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientOrderDto {

    private long   clientId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private boolean enabled;
    private long   orderId;
    private String orderStatus;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}