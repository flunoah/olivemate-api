package com.oliveyoung.mate.domain.product.repository;

import com.oliveyoung.mate.domain.product.model.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID id);
    Optional<Product> findByGoodsNo(String goodsNo);
    List<Product> search(String keyword);
}
