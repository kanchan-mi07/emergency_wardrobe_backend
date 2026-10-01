package com.example.emergencywardrobe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @PositiveOrZero(message = "Price cannot be negative")
    private BigDecimal price;

    @PositiveOrZero(message = "Rental price cannot be negative")
    private BigDecimal rentalPricePerDay;

    @PositiveOrZero(message = "Deposit cannot be negative")
    private BigDecimal securityDeposit;

    private String imageUrl;

    @NotNull(message = "Stock is required")
    @PositiveOrZero(message = "Stock cannot be negative")
    private Integer stock;

    private boolean purchasable;
    private boolean rentable;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @Valid
    private List<VariantRequest> variants = new ArrayList<>();

    @Getter
    @Setter
    public static class VariantRequest {
        @NotBlank(message = "Variant size is required")
        private String size;

        @NotNull(message = "Variant stock is required")
        @PositiveOrZero(message = "Variant stock cannot be negative")
        private Integer stock;
    }
}
