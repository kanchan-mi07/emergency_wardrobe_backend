package com.example.emergencywardrobe.service;

import com.example.emergencywardrobe.entity.Order;
import com.example.emergencywardrobe.entity.OrderStatus;
import com.example.emergencywardrobe.entity.PaymentStatus;
import com.example.emergencywardrobe.entity.Rental;
import com.example.emergencywardrobe.entity.RentalStatus;
import com.example.emergencywardrobe.entity.User;
import com.example.emergencywardrobe.exception.PaymentException;
import com.example.emergencywardrobe.exception.ResourceNotFoundException;
import com.example.emergencywardrobe.exception.UnauthorizedException;
import com.example.emergencywardrobe.repository.OrderRepository;
import com.example.emergencywardrobe.repository.RentalRepository;
import com.example.emergencywardrobe.repository.UserRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Razorpay payments for both shop orders and rentals.
 * The amount is ALWAYS computed on the server from stored data - never taken from the client.
 */
@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    public PaymentService(OrderRepository orderRepository, RentalRepository rentalRepository,
                          UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.rentalRepository = rentalRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------ orders

    @Transactional
    public Map<String, Object> createPaymentOrder(String email, Long orderId) {
        Order order = getOwnedOrder(email, orderId);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new PaymentException("This order has already been paid for");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new PaymentException("This order was cancelled");
        }

        Map<String, Object> response = createRazorpayOrder(order.getTotalAmount(), "order_" + order.getId());

        // Remember which Razorpay order belongs to this internal order.
        order.setRazorpayOrderId((String) response.get("razorpayOrderId"));
        orderRepository.save(order);

        response.put("internalOrderId", order.getId());
        return response;
    }

    @Transactional(noRollbackFor = PaymentException.class)
    public void verifyAndConfirmPayment(String email, Long orderId, String razorpayOrderId,
                                        String razorpayPaymentId, String razorpaySignature) {
        Order order = getOwnedOrder(email, orderId);

        // Already paid: never downgrade it, just treat the repeat call as success.
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }

        // The signed Razorpay order must be the one we created for THIS order.
        if (order.getRazorpayOrderId() == null || !order.getRazorpayOrderId().equals(razorpayOrderId)) {
            throw new PaymentException("Payment does not match this order");
        }

        if (!signatureIsValid(razorpayOrderId, razorpayPaymentId, razorpaySignature)) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new PaymentException("Payment verification failed");
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    // ----------------------------------------------------------------- rentals

    /** Charges rental amount + refundable security deposit. */
    @Transactional
    public Map<String, Object> createRentalPayment(String email, Long rentalId) {
        Rental rental = getOwnedRental(email, rentalId);

        if (rental.getPaymentStatus() == PaymentStatus.PAID) {
            throw new PaymentException("This rental has already been paid for");
        }
        if (rental.getStatus() == RentalStatus.CANCELLED) {
            throw new PaymentException("This rental was cancelled");
        }

        BigDecimal total = rental.getRentalAmount().add(rental.getSecurityDeposit());
        Map<String, Object> response = createRazorpayOrder(total, "rental_" + rental.getId());

        rental.setRazorpayOrderId((String) response.get("razorpayOrderId"));
        rentalRepository.save(rental);

        response.put("internalOrderId", rental.getId());
        return response;
    }

    @Transactional(noRollbackFor = PaymentException.class)
    public void verifyAndConfirmRentalPayment(String email, Long rentalId, String razorpayOrderId,
                                              String razorpayPaymentId, String razorpaySignature) {
        Rental rental = getOwnedRental(email, rentalId);

        if (rental.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }

        if (rental.getRazorpayOrderId() == null || !rental.getRazorpayOrderId().equals(razorpayOrderId)) {
            throw new PaymentException("Payment does not match this rental");
        }

        if (!signatureIsValid(razorpayOrderId, razorpayPaymentId, razorpaySignature)) {
            rental.setPaymentStatus(PaymentStatus.FAILED);
            rentalRepository.save(rental);
            throw new PaymentException("Payment verification failed");
        }

        rental.setPaymentStatus(PaymentStatus.PAID);
        if (rental.getStatus() == RentalStatus.PENDING) {
            rental.setStatus(RentalStatus.CONFIRMED);
        }
        rentalRepository.save(rental);
    }

    // ----------------------------------------------------------------- helpers

    private Map<String, Object> createRazorpayOrder(BigDecimal amount, String receipt) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt);

            com.razorpay.Order razorpayOrder = client.orders.create(orderRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("razorpayOrderId", razorpayOrder.get("id").toString());
            response.put("amount", amountInPaise);
            response.put("currency", "INR");
            response.put("keyId", keyId);
            return response;
        } catch (RazorpayException e) {
            throw new PaymentException("Could not create Razorpay order: " + e.getMessage());
        }
    }

    private boolean signatureIsValid(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);
            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            return false;
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Order getOwnedOrder(String email, Long orderId) {
        User user = getUser(email);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("This order does not belong to you");
        }
        return order;
    }

    private Rental getOwnedRental(String email, Long rentalId) {
        User user = getUser(email);
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found"));
        if (!rental.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("This rental does not belong to you");
        }
        return rental;
    }
}