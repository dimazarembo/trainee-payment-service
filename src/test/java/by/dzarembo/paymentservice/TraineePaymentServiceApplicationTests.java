package by.dzarembo.paymentservice;

import by.dzarembo.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration," +
                        "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration," +
                        "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
        }
)
class TraineePaymentServiceApplicationTests {
    @MockitoBean
    PaymentRepository paymentRepository;

    @MockitoBean
    MongoTemplate mongoTemplate;

    @Test
    void contextLoads() {
    }

}
