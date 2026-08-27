package com.oliveyoung.mate.infrastructure.product.persistence;

import com.oliveyoung.mate.domain.product.model.Product;
import com.oliveyoung.mate.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpaRepository;
    private final ProductMapper mapper;

    @Override
    public Product save(Product product) {
        ProductJpaEntity saved = jpaRepository.save(mapper.toJpa(product));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Product> findByGoodsNo(String goodsNo) {
        return jpaRepository.findByGoodsNo(goodsNo).map(mapper::toDomain);
    }

    @Override
    public List<Product> search(String keyword) {
        return jpaRepository.findTop20ByNameContainingIgnoreCaseOrBrandContainingIgnoreCaseOrderByNameAsc(keyword, keyword).stream()
            .map(mapper::toDomain)
            .toList();
    }
}
