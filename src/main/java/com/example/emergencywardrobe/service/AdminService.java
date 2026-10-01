package com.example.emergencywardrobe.service;

import com.example.emergencywardrobe.dto.*;
import com.example.emergencywardrobe.entity.*;
import com.example.emergencywardrobe.exception.BadRequestException;
import com.example.emergencywardrobe.exception.ResourceNotFoundException;
import com.example.emergencywardrobe.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminService {

    private static final Map<OrderStatus, Set<OrderStatus>> ORDER_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    private static final Map<RentalStatus, Set<RentalStatus>> RENTAL_TRANSITIONS = Map.of(
            RentalStatus.PENDING, Set.of(RentalStatus.CONFIRMED, RentalStatus.CANCELLED),
            RentalStatus.CONFIRMED, Set.of(RentalStatus.ACTIVE, RentalStatus.CANCELLED),
            RentalStatus.ACTIVE, Set.of(RentalStatus.RETURNED),
            RentalStatus.RETURNED, Set.of(RentalStatus.CLEANING),
            RentalStatus.CLEANING, Set.of(),
            RentalStatus.CANCELLED, Set.of()
    );

    private final OrderRepository orderRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public AdminService(OrderRepository orderRepository, RentalRepository rentalRepository,
                        UserRepository userRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.rentalRepository = rentalRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminOrderDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(o -> new AdminOrderDto(o.getUser().getEmail(), OrderDto.fromEntity(o)))
                .toList();
    }

    @Transactional
    public AdminOrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!ORDER_TRANSITIONS.get(order.getStatus()).contains(newStatus)) {
            throw new BadRequestException("Cannot change order from " + order.getStatus() + " to " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            restock(order);
        }
        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        return new AdminOrderDto(saved.getUser().getEmail(), OrderDto.fromEntity(saved));
    }

    // Puts the stock deducted at checkout back. Note: this does NOT trigger a Razorpay refund;
    // refund paid orders manually from the Razorpay dashboard.
    private void restock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (item.getVariantSize() != null) {
                product.getVariants().stream()
                        .filter(v -> v.getSize().equals(item.getVariantSize()))
                        .findFirst()
                        .ifPresent(v -> {
                            v.setStock(v.getStock() + item.getQuantity());
                            v.setAvailable(true);
                        });
            } else {
                product.setStock(product.getStock() + item.getQuantity());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AdminRentalDto> getAllRentals() {
        return rentalRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> new AdminRentalDto(r.getUser().getEmail(), RentalDto.fromEntity(r)))
                .toList();
    }

    @Transactional
    public AdminRentalDto updateRentalStatus(Long rentalId, RentalStatus newStatus) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found"));

        if (!RENTAL_TRANSITIONS.get(rental.getStatus()).contains(newStatus)) {
            throw new BadRequestException("Cannot change rental from " + rental.getStatus() + " to " + newStatus);
        }

        rental.setStatus(newStatus);
        Rental saved = rentalRepository.save(rental);
        return new AdminRentalDto(saved.getUser().getEmail(), RentalDto.fromEntity(saved));
    }

    public List<AdminUserDto> getAllUsers() {
        return userRepository.findAll().stream().map(AdminUserDto::fromEntity).toList();
    }

    public DashboardStatsDto getStats() {
        return new DashboardStatsDto(
                userRepository.count(),
                productRepository.count(),
                orderRepository.count(),
                orderRepository.countByStatus(OrderStatus.PENDING),
                rentalRepository.count(),
                rentalRepository.countByStatus(RentalStatus.ACTIVE),
                orderRepository.sumTotalByPaymentStatus(PaymentStatus.PAID)
        );
    }
}