package by.dzarembo.paymentservice.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDocument {
    @Id
    private String id;

    @Field("order_id")
    @Indexed
    private Long orderId;

    @Indexed
    @Field("user_id")
    private Long userId;

    private PaymentStatus status;

    private Instant timestamp;

    @Field(value = "payment_amount", targetType = FieldType.DECIMAL128)
    private BigDecimal paymentAmount;


}
