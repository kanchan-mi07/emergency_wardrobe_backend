package com.example.emergencywardrobe.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRentalPaymentRequest {

    @NotNull(message = "Rental id is required")
    private Long rentalId;
}