package com.example.emergencywardrobe.controller;

import com.example.emergencywardrobe.dto.CreatePaymentRequest;
import com.example.emergencywardrobe.dto.VerifyPaymentRequest;
import com.example.emergencywardrobe.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public Map<String, Object> createPayment(Authentication authentication,
                                             @Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPaymentOrder(authentication.getName(), request.getOrderId());
    }

    @PostMapping("/verify")
    public Map<String, String> verifyPayment(Authentication authentication,
                                             @Valid @RequestBody VerifyPaymentRequest request) {
        paymentService.verifyAndConfirmPayment(
                authentication.getName(),
                request.getOrderId(),
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
        return Map.of("status", "success");
    }
}
