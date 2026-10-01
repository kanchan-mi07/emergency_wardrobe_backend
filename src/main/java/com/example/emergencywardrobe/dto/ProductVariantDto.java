package com.example.emergencywardrobe.dto;

import com.example.emergencywardrobe.entity.ProductVarient;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductVariantDto {
    private Long id;
    private String size;
    private Integer stock;
    private boolean available;

    public static ProductVariantDto fromEntity(ProductVarient variant) {
        return new ProductVariantDto(
                variant.getId(),
                variant.getSize(),
                variant.getStock(),
                variant.isAvailable()
        );
    }
}