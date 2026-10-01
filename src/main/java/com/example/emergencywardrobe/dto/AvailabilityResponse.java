package com.example.emergencywardrobe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AvailabilityResponse {
    private boolean available;
    private String reason;
}