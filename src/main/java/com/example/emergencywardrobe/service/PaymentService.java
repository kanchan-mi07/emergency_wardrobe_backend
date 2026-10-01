package com.example.emergencywardrobe.service;

import com.example.emergencywardrobe.entity.Order;
import com.example.emergencywardrobe.entity.OrderStatus;
import com.example.emergencywardrobe.entity.PaymentStatus;
import com.example.emergencywardrobe.entity.User;
import com.example.emergencywardrobe.exception.PaymentException;
import com.example.emergencywardrobe.exception.ResourceNotFoundException;
import com.example.emergencywardrobe.exception.UnauthorizedException;
import com.example.emergencywardrobe.repository.OrderRepository;
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

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    public PaymentService(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, Object> createPaymentOrder(String email, Long orderId) {
        Order order = getOwnedOrder(email, orderId);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new PaymentException("This order has already been paid for");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new PaymentException("This order was cancelled");
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            long amountInPaise = order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_" + order.getId());

            com.razorpay.Order razorpayOrder = client.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id").toString();

            // Remember which Razorpay order belongs to this internal order.
            order.setRazorpayOrderId(razorpayOrderId);
            orderRepository.save(order);

            Map<String, Object> response = new HashMap<>();
            response.put("razorpayOrderId", razorpayOrderId);
            response.put("amount", amountInPaise);
            response.put("currency", "INR");
            response.put("keyId", keyId);
            response.put("internalOrderId", order.getId());
            return response;

        } catch (RazorpayException e) {
            throw new PaymentException("Could not create Razorpay order: " + e.getMessage());
        }
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

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            if (!Utils.verifyPaymentSignature(options, keySecret)) {
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
                throw new PaymentException("Payment verification failed");
            }

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

        } catch (RazorpayException e) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new PaymentException("Payment verification failed");
        }
    }

    private Order getOwnedOrder(String email, Long orderId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("This order does not belong to you");
        }
        return order;
    }
}