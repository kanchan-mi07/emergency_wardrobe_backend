package com.example.emergencywardrobe.dto;

import com.example.emergencywardrobe.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private List<OrderItemDto> items;
    private BigDecimal totalAmount;
    private String status;
    private String paymentStatus;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddressLine;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;
    private LocalDateTime createdAt;

    public static OrderDto fromEntity(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(OrderItemDto::fromEntity)
                .toList();

        return new OrderDto(
                order.getId(),
                items,
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getShippingName(),
                order.getShippingPhone(),
                order.getShippingAddressLine(),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingPincode(),
                order.getCreatedAt()
        );
    }
}