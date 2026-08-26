package com.oliveyoung.mate.application.product;

import com.oliveyoung.mate.domain.point.vo.Money;
import com.oliveyoung.mate.domain.product.model.Product;
import com.oliveyoung.mate.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductSyncServiceTest {

    private ProductRepository productRepository;
    private ProductSyncService productSyncService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productSyncService = new ProductSyncService(productRepository);
    }

    @Test
    @DisplayName("goodsNo가 없던 상품은 신규 생성된다")
    void createsNewProductWhenGoodsNoUnseen() {
        when(productRepository.findByGoodsNo("A0000123"))
            .thenReturn(Optional.empty());

        int synced = productSyncService.syncAll(
            List.of(new ProductUploadItem("A0000123", "닥터자르트", "시카페어 크림", 38000, 32000)));

        assertThat(synced).isEqualTo(1);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("이미 있는 goodsNo는 브랜드/상품명/가격이 갱신된다")
    void updatesFieldsWhenGoodsNoExists() {
        Product existing = Product.create(
            "A0000123", "닥터자르트", "시카페어 크림", Money.of(38000), Money.of(30000), null);
        when(productRepository.findByGoodsNo("A0000123"))
            .thenReturn(Optional.of(existing));

        productSyncService.syncAll(
            List.of(new ProductUploadItem("A0000123", "닥터자르트", "시카페어 크림", 38000, 32000)));

        assertThat(existing.getSalePrice()).isEqualTo(Money.of(32000));
        verify(productRepository).save(existing);
    }

    @Test
    @DisplayName("한 건 실패해도 나머지는 계속 동기화된다")
    void continuesAfterOneItemFails() {
        when(productRepository.findByGoodsNo(eq("BAD")))
            .thenThrow(new RuntimeException("boom"));
        when(productRepository.findByGoodsNo(eq("GOOD")))
            .thenReturn(Optional.empty());

        int synced = productSyncService.syncAll(List.of(
            new ProductUploadItem("BAD", "브랜드", "실패 상품", 1000, 1000),
            new ProductUploadItem("GOOD", "브랜드", "성공 상품", 2000, 2000)));

        assertThat(synced).isEqualTo(1);
        verify(productRepository, times(1)).save(any(Product.class));
    }
}
