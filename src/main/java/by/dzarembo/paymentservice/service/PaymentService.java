package by.dzarembo.paymentservice.service;

import by.dzarembo.paymentservice.client.RandomNumberClient;
import by.dzarembo.paymentservice.document.PaymentStatus;
import by.dzarembo.paymentservice.dto.PaymentCreateRequest;
import by.dzarembo.paymentservice.dto.PaymentResponse;
import by.dzarembo.paymentservice.dto.PaymentTotalAggregationResult;
import by.dzarembo.paymentservice.dto.PaymentTotalResponse;
import by.dzarembo.paymentservice.event.PaymentEvent;
import by.dzarembo.paymentservice.mapper.PaymentMapper;
import by.dzarembo.paymentservice.producer.KafkaPaymentEventProducer;
import by.dzarembo.paymentservice.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final MongoTemplate mongoTemplate;
    private final RandomNumberClient randomNumberClient;
    private final KafkaPaymentEventProducer kafkaPaymentEventProducer;

    public PaymentResponse create(PaymentCreateRequest paymentCreateRequest) {
        var paymentDocument = paymentMapper.toEntity(paymentCreateRequest);
        paymentDocument.setTimestamp(Instant.now());
        int number = randomNumberClient.getRandomNumber();
        paymentDocument.setStatus(number % 2 == 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        var savedDocument = paymentRepository.save(paymentDocument);
        var paymentEvent = PaymentEvent.builder()
                .paymentId(savedDocument.getId())
                .orderId(savedDocument.getOrderId())
                .userId(savedDocument.getUserId())
                .paymentStatus(savedDocument.getStatus())
                .occurredAt(Instant.now())
                .build();
        kafkaPaymentEventProducer.sendCreatePaymentEvent(paymentEvent);
        return paymentMapper.toResponse(savedDocument);
    }

    public List<PaymentResponse> getPayments(Long userId, Long orderId, PaymentStatus status) {
        validateExactlyOneFilter(userId, orderId, status);

        if (userId != null) {
            return paymentRepository.findAllByUserId(userId).stream().map(paymentMapper::toResponse).toList();
        }
        if (orderId != null) {
            return paymentRepository.findAllByOrderId(orderId).stream().map(paymentMapper::toResponse).toList();
        }

        return paymentRepository.findAllByStatus(status).stream().map(paymentMapper::toResponse).toList();


    }


    public PaymentTotalResponse getTotalAmountForUser(Long userId, Instant from, Instant to) {
        validateDateRange(from, to);
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("user_id").is(userId),
                Criteria.where("timestamp").gte(from).lte(to)
        );
        return calculateTotalAmount(criteria, from, to);
    }

    public PaymentTotalResponse getTotalAmountForAllUsers(Instant from, Instant to) {
        validateDateRange(from, to);
        Criteria criteria = Criteria.where("timestamp").gte(from).lte(to);
        return calculateTotalAmount(criteria, from, to);
    }

    private PaymentTotalResponse calculateTotalAmount(Criteria criteria, Instant from, Instant to) {
        MatchOperation matchOperation = Aggregation.match(criteria);
        GroupOperation groupOperation = Aggregation.group().sum("payment_amount").as("totalAmount");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);

        AggregationResults<PaymentTotalAggregationResult> results = mongoTemplate.aggregate(
                aggregation,
                "payments",
                PaymentTotalAggregationResult.class
        );

        var result = results.getUniqueMappedResult();
        BigDecimal totalAmount = result != null ? result.getTotalAmount() : BigDecimal.ZERO;

        return PaymentTotalResponse.builder()
                .totalAmount(totalAmount)
                .dateFrom(from)
                .dateTo(to)
                .build();
    }

    private void validateDateRange(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }


    private void validateExactlyOneFilter(Long userId, Long orderId, PaymentStatus status) {
        int filtersCount = (userId != null ? 1 : 0) + (orderId != null ? 1 : 0) + (status != null ? 1 : 0);

        if (filtersCount != 1) {
            throw new IllegalArgumentException(
                    "Exactly one filter must be provided: userId, orderId, or status"
            );
        }
    }
}
