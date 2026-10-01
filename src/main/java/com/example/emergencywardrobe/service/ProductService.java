package com.example.emergencywardrobe.service;

import com.example.emergencywardrobe.dto.ProductDto;
import com.example.emergencywardrobe.dto.ProductRequest;
import com.example.emergencywardrobe.entity.Category;
import com.example.emergencywardrobe.entity.Product;
import com.example.emergencywardrobe.entity.ProductVarient;

import com.example.emergencywardrobe.exception.BadRequestException;
import com.example.emergencywardrobe.exception.ResourceNotFoundException;
import com.example.emergencywardrobe.repository.CategoryRepository;
import com.example.emergencywardrobe.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageSearchService imageSearchService;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          ImageSearchService imageSearchService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.imageSearchService = imageSearchService;
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream().map(ProductDto::fromEntity).toList();
    }

    public ProductDto getProductById(Long id) {
        return ProductDto.fromEntity(findOrThrow(id));
    }

    @Transactional
    public ProductDto createProduct(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        return ProductDto.fromEntity(productRepository.save(product));
    }

    @Transactional
    public ProductDto updateProduct(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        applyRequest(product, request);
        return ProductDto.fromEntity(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(findOrThrow(id));
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
    @Transactional
    public int fillImages(boolean replaceAll) {
        int updated = 0;
        for (Product p : productRepository.findAll()) {
            boolean missing = p.getImageUrl() == null || p.getImageUrl().isBlank();
            if (replaceAll || missing) {
                var url = imageSearchService.findImageUrl(p.getName());
                if (url.isPresent()) {
                    p.setImageUrl(url.get());
                    updated++;
                }
            }
        }
        return updated;
    }

    private void applyRequest(Product product, ProductRequest request) {
        if (!request.isPurchasable() && !request.isRentable()) {
            throw new BadRequestException("A product must be purchasable, rentable, or both");
        }
        if (request.isPurchasable() && request.getPrice() == null) {
            throw new BadRequestException("Price is required for purchasable products");
        }
        if (request.isRentable() && request.getRentalPricePerDay() == null) {
            throw new BadRequestException("Rental price per day is required for rentable products");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setCategory(category);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.isPurchasable() ? request.getPrice() : null);
        product.setRentalPricePerDay(request.isRentable() ? request.getRentalPricePerDay() : null);
        product.setSecurityDeposit(request.isRentable() ? request.getSecurityDeposit() : null);
        String imageUrl = request.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = (product.getImageUrl() != null && !product.getImageUrl().isBlank())
                    ? product.getImageUrl()
                    : imageSearchService.findImageUrl(request.getName()).orElse(null);
        }
        product.setImageUrl(imageUrl);
        product.setStock(request.getStock());
        product.setPurchasable(request.isPurchasable());
        product.setRentable(request.isRentable());

        syncVariants(product, request.getVariants());
    }

    // Match variants by size so existing variant IDs (referenced by carts/rentals) stay stable.
    private void syncVariants(Product product, List<ProductRequest.VariantRequest> requested) {
        product.getVariants().removeIf(existing ->
                requested.stream().noneMatch(r -> r.getSize().equalsIgnoreCase(existing.getSize())));

        for (ProductRequest.VariantRequest r : requested) {
            ProductVarient match = product.getVariants().stream()
                    .filter(v -> v.getSize().equalsIgnoreCase(r.getSize()))
                    .findFirst().orElse(null);

            if (match != null) {
                match.setStock(r.getStock());
                match.setAvailable(r.getStock() > 0);
            } else {
                product.getVariants().add(new ProductVarient(product, r.getSize().trim(), r.getStock()));
            }
        }
    }
}