package com.example.emergencywardrobe.repository;

import com.example.emergencywardrobe.entity.Rental;
import com.example.emergencywardrobe.entity.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    List<Rental> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT r FROM Rental r WHERE r.product.id = :productId " +
            "AND ((:variantId IS NULL AND r.variant IS NULL) OR (r.variant.id = :variantId)) " +
            "AND r.status IN :blockingStatuses " +
            "AND r.startDate < :endDate AND r.endDate > :startDate")
    List<Rental> findOverlapping(@Param("productId") Long productId,
                                 @Param("variantId") Long variantId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 @Param("blockingStatuses") List<RentalStatus> blockingStatuses);
    List<Rental> findAllByOrderByCreatedAtDesc();
    long countByStatus(RentalStatus status);
}