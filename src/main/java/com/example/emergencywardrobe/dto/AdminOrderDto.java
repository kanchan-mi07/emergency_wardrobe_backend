package com.example.emergencywardrobe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminOrderDto {
    private String userEmail;
    private OrderDto order;
}