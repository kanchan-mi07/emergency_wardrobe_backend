package com.example.emergencywardrobe.service;

import com.example.emergencywardrobe.dto.AddCartItemRequest;
import com.example.emergencywardrobe.dto.CartDto;
import com.example.emergencywardrobe.dto.UpdateCartItemRequest;
import com.example.emergencywardrobe.entity.*;

import com.example.emergencywardrobe.exception.BadRequestException;
import com.example.emergencywardrobe.exception.ResourceNotFoundException;
import com.example.emergencywardrobe.exception.UnauthorizedException;
import com.example.emergencywardrobe.repository.CartItemRepository;
import com.example.emergencywardrobe.repository.CartRepository;
import com.example.emergencywardrobe.repository.ProductRepository;
import com.example.emergencywardrobe.repository.ProductVariantRepository;
import com.example.emergencywardrobe.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       ProductRepository productRepository, ProductVariantRepository productVariantRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
    }

    private Cart getOrCreateCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    public CartDto getCart(String email) {
        return CartDto.fromEntity(getOrCreateCart(email));
    }

    public CartDto addItem(String email, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(email);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isPurchasable()) {
            throw new BadRequestException(
                    "This product is rent-only and can't be added to the cart. Use the rental flow instead.");
        }

        ProductVarient varient = null;
        int availableStock = product.getStock();

        if (request.getVariantId() != null) {
            varient = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
            availableStock = varient.getStock();
        }

        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();

        Long variantId = varient == null ? null : varient.getId();
        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductIdAndVariantId(cart.getId(), product.getId(), variantId)
                .orElse(null);

        int requestedTotalQuantity = quantity + (existingItem == null ? 0 : existingItem.getQuantity());
        if (requestedTotalQuantity > availableStock) {
            throw new BadRequestException("Only " + availableStock + " left in stock");
        }

        if (existingItem != null) {
            existingItem.setQuantity(requestedTotalQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem item = new CartItem(cart, product, varient, quantity);
            cartItemRepository.save(item);
        }

        return CartDto.fromEntity(cartRepository.findById(cart.getId()).orElseThrow());
    }

    public CartDto updateItemQuantity(String email, Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(email);
        CartItem item = getOwnedItemOrThrow(cart, itemId);

        int newQuantity = request.getQuantity();
        int availableStock = item.getVariant() != null ? item.getVariant().getStock() : item.getProduct().getStock();

        if (newQuantity > availableStock) {
            throw new BadRequestException("Only " + availableStock + " left in stock");
        }

        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        return CartDto.fromEntity(cartRepository.findById(cart.getId()).orElseThrow());
    }

    public CartDto removeItem(String email, Long itemId) {
        Cart cart = getOrCreateCart(email);
        CartItem item = getOwnedItemOrThrow(cart, itemId);

        cartItemRepository.delete(item);

        return CartDto.fromEntity(cartRepository.findById(cart.getId()).orElseThrow());
    }

    private CartItem getOwnedItemOrThrow(Cart cart, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new UnauthorizedException("This cart item does not belong to you");
        }
        return item;
    }
}
