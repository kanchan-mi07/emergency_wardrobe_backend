package com.example.emergencywardrobe.controller;

import com.example.emergencywardrobe.dto.AddressDto;
import com.example.emergencywardrobe.dto.AddressRequest;
import com.example.emergencywardrobe.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public List<AddressDto> getMyAddresses(Authentication authentication) {
        return addressService.getMyAddresses(authentication.getName());
    }

    @PostMapping
    public AddressDto addAddress(Authentication authentication, @Valid @RequestBody AddressRequest request) {
        return addressService.addAddress(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    public AddressDto updateAddress(Authentication authentication, @PathVariable Long id,
                                    @Valid @RequestBody AddressRequest request) {
        return addressService.updateAddress(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteAddress(Authentication authentication, @PathVariable Long id) {
        addressService.deleteAddress(authentication.getName(), id);
    }
}
