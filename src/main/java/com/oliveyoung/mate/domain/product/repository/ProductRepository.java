package com.oliveyoung.mate.domain.product.repository;

import com.oliveyoung.mate.domain.product.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findByGoodsNo(String goodsNo);
    List<Product> search(String keyword);
}
