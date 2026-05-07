package by.dzarembo.paymentservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTotalResponse {
    private BigDecimal totalAmount;
    private Instant dateFrom;
    private Instant dateTo;
}
