package by.dzarembo.paymentservice.controller;

import by.dzarembo.paymentservice.document.PaymentStatus;
import by.dzarembo.paymentservice.dto.PaymentCreateRequest;
import by.dzarembo.paymentservice.dto.PaymentResponse;
import by.dzarembo.paymentservice.dto.PaymentTotalResponse;
import by.dzarembo.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentCreateRequest paymentCreateRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(paymentCreateRequest));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getPayments(@RequestParam(required = false) Long userId,
                                                             @RequestParam(required = false) Long orderId,
                                                             @RequestParam(required = false) PaymentStatus status) {
        return ResponseEntity.ok(paymentService.getPayments(userId, orderId, status));
    }

    @GetMapping("/{userId}/total")
    public ResponseEntity<PaymentTotalResponse> getPaymentTotalForUser(@PathVariable Long userId, @RequestParam Instant from, @RequestParam Instant to) {
        return ResponseEntity.ok(paymentService.getTotalAmountForUser(userId, from, to));
    }

    @GetMapping("/all")
    public ResponseEntity<PaymentTotalResponse> getPaymentTotalForAllUsers(@RequestParam Instant from, @RequestParam Instant to) {
        return ResponseEntity.ok(paymentService.getTotalAmountForAllUsers(from, to));
    }
}
