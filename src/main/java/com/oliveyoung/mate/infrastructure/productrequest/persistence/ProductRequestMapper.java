package com.oliveyoung.mate.infrastructure.productrequest.persistence;

import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import com.oliveyoung.mate.domain.productrequest.model.ProductRequest;
import org.springframework.stereotype.Component;

@Component
public class ProductRequestMapper {

    public ProductRequest toDomain(ProductRequestJpaEntity entity) {
        return ProductRequest.reconstruct(
            entity.getId(),
            CrewId.of(entity.getCrewId()),
            entity.getRequestType(),
            entity.getProductName(),
            entity.getBrand(),
            entity.getPrice() == null ? null : Money.of(entity.getPrice()),
            entity.getNote(),
            entity.getLinkedProductId(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getReviewedAt()
        );
    }

    public ProductRequestJpaEntity toJpa(ProductRequest request) {
        return ProductRequestJpaEntity.builder()
            .id(request.getId())
            .crewId(request.getCrewId().id())
            .requestType(request.getRequestType())
            .productName(request.getProductName())
            .brand(request.getBrand())
            .price(request.getPrice() == null ? null : request.getPrice().amount())
            .note(request.getNote())
            .linkedProductId(request.getLinkedProductId())
            .status(request.getStatus())
            .createdAt(request.getCreatedAt())
            .reviewedAt(request.getReviewedAt())
            .build();
    }
}
