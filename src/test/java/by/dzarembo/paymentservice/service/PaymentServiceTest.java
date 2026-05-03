package by.dzarembo.paymentservice.service;

import by.dzarembo.paymentservice.client.RandomNumberClient;
import by.dzarembo.paymentservice.document.PaymentDocument;
import by.dzarembo.paymentservice.document.PaymentStatus;
import by.dzarembo.paymentservice.dto.PaymentCreateRequest;
import by.dzarembo.paymentservice.dto.PaymentResponse;
import by.dzarembo.paymentservice.dto.PaymentTotalAggregationResult;
import by.dzarembo.paymentservice.dto.PaymentTotalResponse;
import by.dzarembo.paymentservice.event.PaymentEvent;
import by.dzarembo.paymentservice.mapper.PaymentMapper;
import by.dzarembo.paymentservice.producer.KafkaPaymentEventProducer;
import by.dzarembo.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private RandomNumberClient randomNumberClient;

    @Mock
    private KafkaPaymentEventProducer kafkaPaymentEventProducer;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void create_shouldCreateSuccessPayment_whenRandomNumberEven() {
        PaymentCreateRequest request = buildCreateRequest();

        PaymentDocument paymentDocument = buildPaymentDocument();
        PaymentDocument savedPaymentDocument = buildSavedPaymentDocument(PaymentStatus.SUCCESS);
        PaymentResponse response = buildPaymentResponse(PaymentStatus.SUCCESS);

        when(paymentMapper.toEntity(request)).thenReturn(paymentDocument);
        when(randomNumberClient.getRandomNumber()).thenReturn(2);
        when(paymentRepository.save(paymentDocument)).thenReturn(savedPaymentDocument);
        when(paymentMapper.toResponse(savedPaymentDocument)).thenReturn(response);

        PaymentResponse result = paymentService.create(request);

        assertThat(result).isEqualTo(response);
        assertThat(paymentDocument.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paymentDocument.getTimestamp()).isNotNull();

        verify(paymentMapper).toEntity(request);
        verify(randomNumberClient).getRandomNumber();
        verify(paymentRepository).save(paymentDocument);
        verify(paymentMapper).toResponse(savedPaymentDocument);

        verify(kafkaPaymentEventProducer).sendCreatePaymentEvent(any(PaymentEvent.class));
    }


    @Test
    void create_shouldCreateFailedPayment_whenRandomNumberOdd() {
        PaymentCreateRequest request = buildCreateRequest();

        PaymentDocument paymentDocument = buildPaymentDocument();
        PaymentDocument savedDocument = buildSavedPaymentDocument(PaymentStatus.FAILED);
        PaymentResponse response = buildPaymentResponse(PaymentStatus.FAILED);

        when(paymentMapper.toEntity(request)).thenReturn(paymentDocument);
        when(randomNumberClient.getRandomNumber()).thenReturn(1);
        when(paymentRepository.save(paymentDocument)).thenReturn(savedDocument);
        when(paymentMapper.toResponse(savedDocument)).thenReturn(response);

        PaymentResponse result = paymentService.create(request);

        assertThat(result).isEqualTo(response);
        assertThat(paymentDocument.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(paymentDocument.getTimestamp()).isNotNull();

        verify(paymentRepository).save(paymentDocument);

        verify(kafkaPaymentEventProducer).sendCreatePaymentEvent(any(PaymentEvent.class));
    }

    @Test
    void getPayments_shouldReturnPayments_whenUserIdProvided() {
        Long userId = 1L;

        PaymentDocument document = buildSavedPaymentDocument(PaymentStatus.SUCCESS);
        PaymentResponse response = buildPaymentResponse(PaymentStatus.SUCCESS);

        when(paymentRepository.findAllByUserId(userId)).thenReturn(List.of(document));
        when(paymentMapper.toResponse(document)).thenReturn(response);

        List<PaymentResponse> result = paymentService.getPayments(userId, null, null);

        assertThat(result).containsExactly(response);

        verify(paymentRepository).findAllByUserId(userId);
        verify(paymentMapper).toResponse(document);
    }

    @Test
    void getPayments_shouldReturnPayments_whenOrderIdProvided() {
        Long orderId = 1L;

        PaymentDocument document = buildSavedPaymentDocument(PaymentStatus.SUCCESS);
        PaymentResponse response = buildPaymentResponse(PaymentStatus.SUCCESS);

        when(paymentRepository.findAllByOrderId(orderId)).thenReturn(List.of(document));
        when(paymentMapper.toResponse(document)).thenReturn(response);

        List<PaymentResponse> result = paymentService.getPayments(null, orderId, null);

        assertThat(result).containsExactly(response);

        verify(paymentRepository).findAllByOrderId(orderId);
        verify(paymentMapper).toResponse(document);
    }

    @Test
    void getPayments_shouldReturnPayments_whenStatusProvided() {
        PaymentStatus status = PaymentStatus.SUCCESS;

        PaymentDocument document = buildSavedPaymentDocument(status);
        PaymentResponse response = buildPaymentResponse(status);

        when(paymentRepository.findAllByStatus(status)).thenReturn(List.of(document));
        when(paymentMapper.toResponse(document)).thenReturn(response);

        List<PaymentResponse> result = paymentService.getPayments(null, null, status);

        assertThat(result).containsExactly(response);

        verify(paymentRepository).findAllByStatus(status);
        verify(paymentMapper).toResponse(document);
    }

    @Test
    void getPayments_shouldThrowException_whenNoFilterProvided() {
        assertThatThrownBy(() -> paymentService.getPayments(null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getPayments_shouldThrowException_whenMoreThanOneFilterProvided() {
        assertThatThrownBy(() -> paymentService.getPayments(1L, 1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTotalAmountForUser_shouldReturnTotalAmount_whenDateRangeCorrect() {
        Long userId = 2L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T00:00:00Z");

        AggregationResults<PaymentTotalAggregationResult> aggregationResults = mock(AggregationResults.class);
        PaymentTotalAggregationResult aggregationResult = new PaymentTotalAggregationResult(BigDecimal.valueOf(300));

        when(mongoTemplate.aggregate(
                any(Aggregation.class),
                eq("payments"),
                eq(PaymentTotalAggregationResult.class)
        )).thenReturn(aggregationResults);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(aggregationResult);

        PaymentTotalResponse result = paymentService.getTotalAmountForUser(userId, from, to);

        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(300));
        assertThat(result.getDateFrom()).isEqualTo(from);
        assertThat(result.getDateTo()).isEqualTo(to);

        verify(mongoTemplate).aggregate(
                any(Aggregation.class),
                eq("payments"),
                eq(PaymentTotalAggregationResult.class)
        );
    }

    @Test
    void getTotalAmountForUser_shouldThrowException_whenFromAfterTo() {
        Instant from = Instant.parse("2026-02-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T00:00:00Z");

        assertThatThrownBy(() -> paymentService.getTotalAmountForUser(1L, from, to))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private PaymentResponse buildPaymentResponse(PaymentStatus status) {
        return PaymentResponse.builder()
                .id("id1")
                .orderId(1L)
                .userId(2L)
                .paymentAmount(BigDecimal.valueOf(100))
                .status(status)
                .build();
    }

    private PaymentDocument buildSavedPaymentDocument(PaymentStatus status) {
        return PaymentDocument.builder()
                .id("id1")
                .orderId(1L)
                .userId(2L)
                .paymentAmount(BigDecimal.valueOf(100))
                .status(status)
                .build();
    }

    private PaymentDocument buildPaymentDocument() {
        return PaymentDocument.builder()
                .orderId(1L)
                .userId(2L)
                .paymentAmount(BigDecimal.TEN)
                .build();
    }

    private PaymentCreateRequest buildCreateRequest() {
        return PaymentCreateRequest.builder()
                .orderId(1L)
                .userId(2L)
                .paymentAmount(BigDecimal.TEN)
                .build();
    }

}
