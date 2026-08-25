package com.oliveyoung.mate.infrastructure.product.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {
    Optional<ProductJpaEntity> findByGoodsNo(String goodsNo);
    List<ProductJpaEntity> findTop20ByNameContainingIgnoreCaseOrderByNameAsc(String keyword);
}
