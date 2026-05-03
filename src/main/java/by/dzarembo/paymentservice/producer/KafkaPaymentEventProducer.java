package by.dzarembo.paymentservice.producer;

import by.dzarembo.paymentservice.event.PaymentEvent;
import by.dzarembo.paymentservice.event.PaymentEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaPaymentEventProducer {
    public static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void sendCreatePaymentEvent(PaymentEvent paymentEvent) {
        paymentEvent.setEventType(PaymentEventType.CREATE_PAYMENT);

        kafkaTemplate.send(
                TOPIC,
                String.valueOf(paymentEvent.getOrderId()),
                paymentEvent
        );
    }
}
