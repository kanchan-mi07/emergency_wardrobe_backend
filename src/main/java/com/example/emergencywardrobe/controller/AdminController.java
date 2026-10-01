package com.example.emergencywardrobe.controller;

import com.example.emergencywardrobe.dto.*;
import com.example.emergencywardrobe.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public DashboardStatsDto stats() {
        return adminService.getStats();
    }

    @GetMapping("/orders")
    public List<AdminOrderDto> orders() {
        return adminService.getAllOrders();
    }

    @PutMapping("/orders/{id}/status")
    public AdminOrderDto updateOrderStatus(@PathVariable Long id,
                                           @Valid @RequestBody UpdateOrderStatusRequest request) {
        return adminService.updateOrderStatus(id, request.getStatus());
    }

    @GetMapping("/rentals")
    public List<AdminRentalDto> rentals() {
        return adminService.getAllRentals();
    }

    @PutMapping("/rentals/{id}/status")
    public AdminRentalDto updateRentalStatus(@PathVariable Long id,
                                             @Valid @RequestBody UpdateRentalStatusRequest request) {
        return adminService.updateRentalStatus(id, request.getStatus());
    }

    @GetMapping("/users")
    public List<AdminUserDto> users() {
        return adminService.getAllUsers();
    }
}