package com.example.emergencywardrobe.service;

import com.example.emergencywardrobe.dto.AvailabilityResponse;
import com.example.emergencywardrobe.dto.CheckAvailabilityRequest;
import com.example.emergencywardrobe.dto.CreateRentalRequest;
import com.example.emergencywardrobe.dto.RentalDto;
import com.example.emergencywardrobe.entity.*;
import com.example.emergencywardrobe.exception.BadRequestException;
import com.example.emergencywardrobe.exception.RentalNotAvailableException;
import com.example.emergencywardrobe.exception.ResourceNotFoundException;
import com.example.emergencywardrobe.exception.UnauthorizedException;
import com.example.emergencywardrobe.repository.ProductRepository;
import com.example.emergencywardrobe.repository.ProductVariantRepository;
import com.example.emergencywardrobe.repository.RentalRepository;
import com.example.emergencywardrobe.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RentalService {

    private static final List<RentalStatus> BLOCKING_STATUSES =
            List.of(RentalStatus.PENDING, RentalStatus.CONFIRMED, RentalStatus.ACTIVE, RentalStatus.CLEANING);

    private final RentalRepository rentalRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public RentalService(RentalRepository rentalRepository, ProductRepository productRepository,
                         ProductVariantRepository productVariantRepository, UserRepository userRepository) {
        this.rentalRepository = rentalRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
    }

    public AvailabilityResponse checkAvailability(CheckAvailabilityRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isRentable()) {
            return new AvailabilityResponse(false, "This product is not available for rent");
        }

        if (request.getVariantId() != null) {
            productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        }

        List<Rental> overlaps = rentalRepository.findOverlapping(
                request.getProductId(), request.getVariantId(),
                request.getStartDate(), request.getEndDate(), BLOCKING_STATUSES);

        if (!overlaps.isEmpty()) {
            return new AvailabilityResponse(false, "Not available for the selected dates");
        }

        return new AvailabilityResponse(true, null);
    }

    @Transactional
    public RentalDto createRental(String email, CreateRentalRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isRentable()) {
            throw new BadRequestException("This product is not available for rent");
        }

        ProductVarient variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        }

        List<Rental> overlaps = rentalRepository.findOverlapping(
                product.getId(), request.getVariantId(),
                request.getStartDate(), request.getEndDate(), BLOCKING_STATUSES);

        if (!overlaps.isEmpty()) {
            throw new RentalNotAvailableException("This product is not available for the selected dates");
        }

        long rentalDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());

        BigDecimal rentalAmount = product.getRentalPricePerDay().multiply(BigDecimal.valueOf(rentalDays));
        BigDecimal securityDeposit = product.getSecurityDeposit() != null
                ? product.getSecurityDeposit() : BigDecimal.ZERO;

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setProduct(product);
        rental.setVariant(variant);
        rental.setProductName(product.getName());
        rental.setVariantSize(variant != null ? variant.getSize() : null);
        rental.setStartDate(request.getStartDate());
        rental.setEndDate(request.getEndDate());
        rental.setRentalAmount(rentalAmount);
        rental.setSecurityDeposit(securityDeposit);
        rental.setStatus(RentalStatus.PENDING);

        Rental saved = rentalRepository.save(rental);
        return RentalDto.fromEntity(saved);
    }

    public List<RentalDto> getMyRentals(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return rentalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(RentalDto::fromEntity)
                .toList();
    }

    public RentalDto getMyRentalById(String email, Long rentalId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found"));

        if (!rental.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("This rental does not belong to you");
        }
        return RentalDto.fromEntity(rental);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new BadRequestException("End date must be after start date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Start date cannot be in the past");
        }
    }
}
