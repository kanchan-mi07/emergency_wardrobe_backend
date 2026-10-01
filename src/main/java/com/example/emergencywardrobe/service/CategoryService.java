package com.example.emergencywardrobe.service;

import com.example.emergencywardrobe.dto.CategoryDto;
import com.example.emergencywardrobe.dto.CategoryRequest;
import com.example.emergencywardrobe.entity.Category;
import com.example.emergencywardrobe.exception.BadRequestException;
import com.example.emergencywardrobe.exception.ResourceNotFoundException;
import com.example.emergencywardrobe.repository.CategoryRepository;
import com.example.emergencywardrobe.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream().map(CategoryDto::fromEntity).toList();
    }

    public CategoryDto createCategory(CategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.findByName(name).isPresent()) {
            throw new BadRequestException("A category with this name already exists");
        }
        return CategoryDto.fromEntity(categoryRepository.save(new Category(name)));
    }

    public CategoryDto updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(request.getName().trim());
        return CategoryDto.fromEntity(categoryRepository.save(category));
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        // Category -> products is cascade ALL, so deleting a non-empty category would wipe its products.
        if (!productRepository.findByCategoryId(id).isEmpty()) {
            throw new BadRequestException("Move or delete this category's products first");
        }
        categoryRepository.delete(category);
    }
}