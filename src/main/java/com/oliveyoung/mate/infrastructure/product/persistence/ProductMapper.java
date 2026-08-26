package com.oliveyoung.mate.infrastructure.product.persistence;

import com.oliveyoung.mate.domain.point.vo.Money;
import com.oliveyoung.mate.domain.product.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toDomain(ProductJpaEntity entity) {
        return Product.reconstruct(
            entity.getProductId(),
            entity.getGoodsNo(),
            entity.getBrand(),
            entity.getName(),
            Money.of(entity.getRegularPrice()),
            Money.of(entity.getSalePrice()),
            entity.getSyncedAt()
        );
    }

    public ProductJpaEntity toJpa(Product product) {
        return ProductJpaEntity.builder()
            .productId(product.getId())
            .goodsNo(product.getGoodsNo())
            .brand(product.getBrand())
            .name(product.getName())
            .regularPrice(product.getRegularPrice().amount())
            .salePrice(product.getSalePrice().amount())
            .syncedAt(product.getSyncedAt())
            .build();
    }
}
