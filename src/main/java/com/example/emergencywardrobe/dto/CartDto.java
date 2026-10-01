package com.example.emergencywardrobe.dto;

import com.example.emergencywardrobe.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class CartDto {
    private Long id;
    private List<CartItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal total;

    public static CartDto fromEntity(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream()
                .map(CartItemDto::fromEntity)
                .toList();

        BigDecimal subtotal = items.stream()
                .map(CartItemDto::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDto(cart.getId(), items, subtotal, subtotal);
    }
}