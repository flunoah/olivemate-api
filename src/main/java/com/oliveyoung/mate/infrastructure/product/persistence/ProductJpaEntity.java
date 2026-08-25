package com.oliveyoung.mate.infrastructure.product.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductJpaEntity {

    @Id
    private UUID productId;

    @Column(nullable = false, length = 50, unique = true)
    private String goodsNo;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private Long regularPrice;

    @Column(nullable = false)
    private Long salePrice;

    @Column(nullable = false)
    private LocalDateTime syncedAt;

    @Builder
    public ProductJpaEntity(UUID productId, String goodsNo, String brand, String name,
                             Long regularPrice, Long salePrice, LocalDateTime syncedAt) {
        this.productId = productId;
        this.goodsNo = goodsNo;
        this.brand = brand;
        this.name = name;
        this.regularPrice = regularPrice;
        this.salePrice = salePrice;
        this.syncedAt = syncedAt;
    }
}
