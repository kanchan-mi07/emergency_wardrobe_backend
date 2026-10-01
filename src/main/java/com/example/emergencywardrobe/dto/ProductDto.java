package com.example.emergencywardrobe.dto;

import com.example.emergencywardrobe.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal rentalPricePerDay;
    private BigDecimal securityDeposit;
    private String imageUrl;
    private Integer stock;
    private boolean purchasable;
    private boolean rentable;
    private String categoryName;
    private List<ProductVariantDto> variants;

    public static ProductDto fromEntity(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getRentalPricePerDay(),
                product.getSecurityDeposit(),
                product.getImageUrl(),
                product.getStock(),
                product.isPurchasable(),
                product.isRentable(),
                product.getCategory().getName(),
                product.getVariants().stream().map(ProductVariantDto::fromEntity).toList()
        );
    }
}