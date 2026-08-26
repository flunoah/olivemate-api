package com.oliveyoung.mate.application.product;

import com.oliveyoung.mate.domain.point.vo.Money;
import com.oliveyoung.mate.domain.product.model.Product;
import com.oliveyoung.mate.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private final ProductRepository productRepository;

    // goodsNo 기준 upsert. 한 건 실패해도 나머지는 계속 진행한다.
    public int syncAll(List<ProductUploadItem> items) {
        int synced = 0;
        for (ProductUploadItem item : items) {
            try {
                syncOne(item);
                synced++;
            } catch (Exception e) {
                log.warn("상품 동기화 실패, 나머지는 계속 진행: goodsNo={}", item.goodsNo(), e);
            }
        }
        return synced;
    }

    private void syncOne(ProductUploadItem item) {
        LocalDateTime now = LocalDateTime.now();
        Product product = productRepository.findByGoodsNo(item.goodsNo())
            .map(existing -> {
                existing.update(item.brand(), item.name(),
                    Money.of(item.regularPrice()), Money.of(item.salePrice()), now);
                return existing;
            })
            .orElseGet(() -> Product.create(item.goodsNo(), item.brand(), item.name(),
                Money.of(item.regularPrice()), Money.of(item.salePrice()), now));
        productRepository.save(product);
    }
}
