package com.oliveyoung.mate.domain.productrequest.repository;

import com.oliveyoung.mate.domain.productrequest.model.ProductRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRequestRepository {
    ProductRequest save(ProductRequest request);
    Optional<ProductRequest> findById(UUID id);
    List<ProductRequest> findAllByStatus(ProductRequest.RequestStatus status);
}
