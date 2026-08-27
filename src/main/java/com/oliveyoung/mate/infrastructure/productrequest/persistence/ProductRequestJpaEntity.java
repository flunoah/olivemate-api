package com.oliveyoung.mate.infrastructure.productrequest.persistence;

import com.oliveyoung.mate.domain.productrequest.model.ProductRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductRequestJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID crewId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductRequest.RequestType requestType;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(length = 100)
    private String brand;

    private Long price;

    @Column(length = 500)
    private String note;

    private UUID linkedProductId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductRequest.RequestStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    @Builder
    public ProductRequestJpaEntity(UUID id, UUID crewId, ProductRequest.RequestType requestType,
                                    String productName, String brand, Long price, String note,
                                    UUID linkedProductId, ProductRequest.RequestStatus status,
                                    LocalDateTime createdAt, LocalDateTime reviewedAt) {
        this.id = id;
        this.crewId = crewId;
        this.requestType = requestType;
        this.productName = productName;
        this.brand = brand;
        this.price = price;
        this.note = note;
        this.linkedProductId = linkedProductId;
        this.status = status;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }
}
