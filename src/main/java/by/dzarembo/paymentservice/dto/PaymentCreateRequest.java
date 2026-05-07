package by.dzarembo.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentCreateRequest {
    @NotNull
    private Long orderId;
    @NotNull
    private Long userId;
    @NotNull
    @Positive
    private BigDecimal paymentAmount;
}
