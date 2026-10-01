package com.example.emergencywardrobe.controller;

import com.example.emergencywardrobe.dto.AddCartItemRequest;
import com.example.emergencywardrobe.dto.CartDto;
import com.example.emergencywardrobe.dto.UpdateCartItemRequest;
import com.example.emergencywardrobe.service.CartService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartDto getCart(Authentication authentication) {
        return cartService.getCart(authentication.getName());
    }

    @PostMapping("/items")
    public CartDto addItem(Authentication authentication, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(authentication.getName(), request);
    }

    @PutMapping("/items/{id}")
    public CartDto updateItem(Authentication authentication, @PathVariable Long id,
                              @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItemQuantity(authentication.getName(), id, request);
    }

    @DeleteMapping("/items/{id}")
    public CartDto removeItem(Authentication authentication, @PathVariable Long id) {
        return cartService.removeItem(authentication.getName(), id);
    }
}