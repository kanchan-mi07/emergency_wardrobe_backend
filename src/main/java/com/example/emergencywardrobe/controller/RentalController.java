package com.example.emergencywardrobe.controller;

import com.example.emergencywardrobe.dto.*;
import com.example.emergencywardrobe.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @PostMapping("/check-availability")
    public AvailabilityResponse checkAvailability(@Valid @RequestBody CheckAvailabilityRequest request) {
        return rentalService.checkAvailability(request);
    }

    @PostMapping
    public RentalDto createRental(Authentication authentication, @Valid @RequestBody CreateRentalRequest request) {
        return rentalService.createRental(authentication.getName(), request);
    }

    @GetMapping
    public List<RentalDto> getMyRentals(Authentication authentication) {
        return rentalService.getMyRentals(authentication.getName());
    }

    @GetMapping("/{id}")
    public RentalDto getMyRentalById(Authentication authentication, @PathVariable Long id) {
        return rentalService.getMyRentalById(authentication.getName(), id);
    }
}