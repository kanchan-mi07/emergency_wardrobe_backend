package com.example.emergencywardrobe.dto;

import com.example.emergencywardrobe.entity.Rental;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@AllArgsConstructor
public class RentalDto {
    private Long id;
    private Long productId;
    private String productName;
    private String variantSize;
    private LocalDate startDate;
    private LocalDate endDate;
    private long rentalDays;
    private BigDecimal rentalAmount;
    private BigDecimal securityDeposit;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;

    public static RentalDto fromEntity(Rental rental) {
        long days = ChronoUnit.DAYS.between(rental.getStartDate(), rental.getEndDate());
        BigDecimal total = rental.getRentalAmount().add(rental.getSecurityDeposit());

        return new RentalDto(
                rental.getId(),
                rental.getProduct().getId(),
                rental.getProductName(),
                rental.getVariantSize(),
                rental.getStartDate(),
                rental.getEndDate(),
                days,
                rental.getRentalAmount(),
                rental.getSecurityDeposit(),
                total,
                rental.getStatus().name(),
                rental.getCreatedAt()
        );
    }
}