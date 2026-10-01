package com.example.emergencywardrobe.dto;

import com.example.emergencywardrobe.entity.RentalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRentalStatusRequest {
    @NotNull(message = "Status is required")
    private RentalStatus status;
}