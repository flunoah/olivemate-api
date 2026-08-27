package com.oliveyoung.mate.infrastructure.productrequest.persistence;

import com.oliveyoung.mate.domain.productrequest.model.ProductRequest;
import com.oliveyoung.mate.domain.productrequest.repository.ProductRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductRequestRepositoryImpl implements ProductRequestRepository {

    private final ProductRequestJpaRepository jpaRepository;
    private final ProductRequestMapper mapper;

    @Override
    public ProductRequest save(ProductRequest request) {
        ProductRequestJpaEntity saved = jpaRepository.save(mapper.toJpa(request));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProductRequest> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ProductRequest> findAllByStatus(ProductRequest.RequestStatus status) {
        return jpaRepository.findByStatusOrderByCreatedAtDesc(status).stream()
            .map(mapper::toDomain)
            .toList();
    }
}
