package com.example.emergencywardrobe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private long pendingOrders;
    private long totalRentals;
    private long activeRentals;
    private BigDecimal paidRevenue;
}