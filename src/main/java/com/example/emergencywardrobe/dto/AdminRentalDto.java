package com.example.emergencywardrobe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminRentalDto {
    private String userEmail;
    private RentalDto rental;
}