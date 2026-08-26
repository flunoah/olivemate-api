package com.oliveyoung.mate.application.product;

import com.oliveyoung.mate.application.product.result.ProductSearchResult;
import com.oliveyoung.mate.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;

    public List<ProductSearchResult> search(String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return productRepository.search(trimmed).stream()
            .map(p -> new ProductSearchResult(
                p.getId(), p.getGoodsNo(), p.getBrand(), p.getName(),
                p.getRegularPrice().amount(), p.getSalePrice().amount()))
            .toList();
    }
}
