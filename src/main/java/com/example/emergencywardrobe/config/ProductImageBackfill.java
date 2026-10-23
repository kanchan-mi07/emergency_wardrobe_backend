package com.example.emergencywardrobe.config;

import com.example.emergencywardrobe.service.ImageSearchService;
import com.example.emergencywardrobe.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * On startup, gives every product that has no image (or only an old random placeholder)
 * a photo that matches its name. Safe to run on every boot: products that already have
 * a real image are left alone.
 */
@Slf4j
@Component
@Order(2)
public class ProductImageBackfill implements CommandLineRunner {

    private final ProductService productService;
    private final ImageSearchService imageSearchService;

    public ProductImageBackfill(ProductService productService, ImageSearchService imageSearchService) {
        this.productService = productService;
        this.imageSearchService = imageSearchService;
    }

    @Override
    public void run(String... args) {
        if (!imageSearchService.isConfigured()) {
            log.info("UNSPLASH_ACCESS_KEY not set - skipping product image lookup (the UI shows themed placeholders).");
            return;
        }
        try {
            int updated = productService.fillImages(false);
            log.info("Product image backfill finished: {} product(s) updated.", updated);
        } catch (Exception e) {
            // Never block application startup because of an image lookup problem.
            log.warn("Product image backfill failed: {}", e.getMessage());
        }
    }
}