package by.dzarembo.paymentservice.integration;

import by.dzarembo.paymentservice.document.PaymentStatus;
import by.dzarembo.paymentservice.dto.PaymentCreateRequest;
import by.dzarembo.paymentservice.dto.PaymentResponse;
import by.dzarembo.paymentservice.producer.KafkaPaymentEventProducer;
import by.dzarembo.paymentservice.repository.PaymentRepository;
import by.dzarembo.paymentservice.service.PaymentService;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mongodb.MongoDBContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class PaymentServiceIntegrationTest {

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @Container
    static final KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka:3.8.1");

    static final WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

    static {
        wireMockServer.start();
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("random-number-api.base-url", wireMockServer::baseUrl);

        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JacksonJsonSerializer");
        registry.add("spring.kafka.producer.properties.spring.json.add.type.headers",
                () -> false);
    }

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        wireMockServer.resetAll();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void create_shouldSaveSuccessPayment_whenRandomApiReturnsEvenNumber() {
        create_shouldSavePaymentAndSendEvent(2, PaymentStatus.SUCCESS, 1L);
    }

    @Test
    void create_shouldSaveFailedPayment_whenRandomApiReturnsOddNumber() {
        create_shouldSavePaymentAndSendEvent(1, PaymentStatus.FAILED, 2L);
    }

    private void create_shouldSavePaymentAndSendEvent(int randomNumber, PaymentStatus expectedStatus, Long orderId) {
        wireMockServer.stubFor(get(urlPathEqualTo("/integers/"))
                .willReturn(ok(String.valueOf(randomNumber))));

        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            PaymentCreateRequest request = buildCreateRequest(orderId);

            PaymentResponse result = paymentService.create(request);

            assertThat(result.getStatus()).isEqualTo(expectedStatus);
            assertThat(result.getId()).isNotBlank();
            assertThat(result.getTimestamp()).isNotNull();

            var payments = paymentRepository.findAll();

            assertThat(payments).hasSize(1);
            assertThat(payments.get(0).getStatus()).isEqualTo(expectedStatus);

            ConsumerRecord<String, String> record = readPaymentEvent(consumer, expectedStatus, orderId);

            assertThat(record.key()).isEqualTo(String.valueOf(orderId));
            assertThat(record.value()).contains("\"eventType\":\"CREATE_PAYMENT\"");
            assertThat(record.value()).contains("\"paymentStatus\":\"" + expectedStatus + "\"");
            assertThat(record.value()).contains("\"orderId\":" + orderId);
            assertThat(record.value()).contains("\"userId\":2");
        }
    }

    private PaymentCreateRequest buildCreateRequest(Long orderId) {
        return PaymentCreateRequest.builder()
                .orderId(orderId)
                .userId(2L)
                .paymentAmount(BigDecimal.valueOf(100))
                .build();
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(KafkaPaymentEventProducer.TOPIC));
        return consumer;
    }

    private ConsumerRecord<String, String> readPaymentEvent(KafkaConsumer<String, String> consumer,
                                                            PaymentStatus status,
                                                            Long orderId) {
        long timeout = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < timeout) {
            var records = consumer.poll(Duration.ofMillis(500));

            for (ConsumerRecord<String, String> record : records) {
                if (record.value().contains("\"paymentStatus\":\"" + status + "\"")
                        && record.value().contains("\"orderId\":" + orderId)) {
                    return record;
                }
            }
        }
        throw new AssertionError("Payment event was not received");
    }

}
