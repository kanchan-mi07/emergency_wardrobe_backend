package com.example.emergencywardrobe.controller;

import com.example.emergencywardrobe.dto.ProductDto;
import com.example.emergencywardrobe.dto.ProductRequest;
import com.example.emergencywardrobe.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }
    @PostMapping("/fill-images")
    public Map<String, Integer> fillImages(@RequestParam(defaultValue = "false") boolean replaceAll) {
        return Map.of("updated", productService.fillImages(replaceAll));
    }

    @PostMapping
    public ProductDto create(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    public ProductDto update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}
