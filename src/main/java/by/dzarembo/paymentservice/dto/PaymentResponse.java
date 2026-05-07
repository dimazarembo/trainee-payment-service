package by.dzarembo.paymentservice.dto;

import by.dzarembo.paymentservice.document.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {
    private String id;
    private Long orderId;
    private Long userId;
    private PaymentStatus status;
    private Instant timestamp;
    private BigDecimal paymentAmount;
}
