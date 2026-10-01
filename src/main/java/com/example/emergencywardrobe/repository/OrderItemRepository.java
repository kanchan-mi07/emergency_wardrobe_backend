package com.example.emergencywardrobe.repository;

import com.example.emergencywardrobe.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}