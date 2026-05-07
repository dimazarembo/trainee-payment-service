package by.dzarembo.paymentservice.repository;

import by.dzarembo.paymentservice.document.PaymentDocument;
import by.dzarembo.paymentservice.document.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentRepository extends MongoRepository<PaymentDocument, String> {

    List<PaymentDocument> findAllByUserId(Long userId);

    List<PaymentDocument> findAllByOrderId(Long orderId);

    List<PaymentDocument> findAllByStatus(PaymentStatus paymentStatus);
}
