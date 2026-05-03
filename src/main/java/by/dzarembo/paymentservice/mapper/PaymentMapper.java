package by.dzarembo.paymentservice.mapper;

import by.dzarembo.paymentservice.document.PaymentDocument;
import by.dzarembo.paymentservice.dto.PaymentCreateRequest;
import by.dzarembo.paymentservice.dto.PaymentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    PaymentDocument toEntity(PaymentCreateRequest paymentCreateRequest);


    PaymentResponse toResponse(PaymentDocument paymentDocument);
}
