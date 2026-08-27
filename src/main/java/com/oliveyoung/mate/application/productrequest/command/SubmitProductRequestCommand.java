package com.oliveyoung.mate.application.productrequest.command;

import com.oliveyoung.mate.domain.productrequest.model.ProductRequest;
import java.util.UUID;

public record SubmitProductRequestCommand(
    UUID crewId,
    ProductRequest.RequestType requestType,
    String productName,
    String brand,
    Long price,
    String note,
    UUID linkedProductId
) {}
