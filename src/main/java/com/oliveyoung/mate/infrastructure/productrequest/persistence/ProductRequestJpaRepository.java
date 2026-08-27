package com.oliveyoung.mate.infrastructure.productrequest.persistence;

import com.oliveyoung.mate.domain.productrequest.model.ProductRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductRequestJpaRepository extends JpaRepository<ProductRequestJpaEntity, UUID> {
    List<ProductRequestJpaEntity> findByStatusOrderByCreatedAtDesc(ProductRequest.RequestStatus status);
}
