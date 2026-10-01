package com.example.emergencywardrobe.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
public class ProductVarient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Kept as String  it covers both clothing sizes ("S","M","L") and shoe sizes ("6","7","8").
    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(nullable = false)
    private boolean available = true;

    public ProductVarient(Product product, String size, Integer stock) {
        this.product = product;
        this.size = size;
        this.stock = stock;
        this.available = stock > 0;
    }
}
