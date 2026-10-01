package com.example.emergencywardrobe.repository;


import com.example.emergencywardrobe.entity.ProductVarient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVarient, Long> {
    List<ProductVarient> findByProductId(Long productId);
}
