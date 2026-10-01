package com.example.emergencywardrobe.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCartItemRequest {

    @NotNull(message = "Product id is required")
    private Long productId;

    private Long variantId;

    @Positive(message = "Quantity must be positive")
    private Integer quantity = 1;
}