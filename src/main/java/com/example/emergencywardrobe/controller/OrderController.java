package com.example.emergencywardrobe.controller;

import com.example.emergencywardrobe.dto.CreateOrderRequest;
import com.example.emergencywardrobe.dto.OrderDto;
import com.example.emergencywardrobe.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderDto checkout(Authentication authentication, @Valid @RequestBody CreateOrderRequest request) {
        return orderService.checkout(authentication.getName(), request);
    }

    @GetMapping
    public List<OrderDto> getMyOrders(Authentication authentication) {
        return orderService.getMyOrders(authentication.getName());
    }

    @GetMapping("/{id}")
    public OrderDto getMyOrderById(Authentication authentication, @PathVariable Long id) {
        return orderService.getMyOrderById(authentication.getName(), id);
    }
}