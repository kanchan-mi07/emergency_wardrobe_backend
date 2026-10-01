package com.example.emergencywardrobe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserSummaryDto {
    private String name;
    private String email;
    private String role;
}