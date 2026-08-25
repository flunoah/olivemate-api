package com.oliveyoung.mate.domain.product.model;

import com.oliveyoung.mate.domain.point.vo.Money;
import java.time.LocalDateTime;
import java.util.UUID;

public class Product {

    private final UUID id;
    private final String goodsNo;
    private String brand;
    private String name;
    private Money regularPrice;
    private Money salePrice;
    private LocalDateTime syncedAt;

    private Product(UUID id, String goodsNo, String brand, String name,
                     Money regularPrice, Money salePrice, LocalDateTime syncedAt) {
        this.id = id;
        this.goodsNo = goodsNo;
        this.brand = brand;
        this.name = name;
        this.regularPrice = regularPrice;
        this.salePrice = salePrice;
        this.syncedAt = syncedAt;
    }

    public static Product create(String goodsNo, String brand, String name,
                                  Money regularPrice, Money salePrice, LocalDateTime syncedAt) {
        return new Product(UUID.randomUUID(), goodsNo, brand, name, regularPrice, salePrice, syncedAt);
    }

    public static Product reconstruct(UUID id, String goodsNo, String brand, String name,
                                       Money regularPrice, Money salePrice, LocalDateTime syncedAt) {
        return new Product(id, goodsNo, brand, name, regularPrice, salePrice, syncedAt);
    }

    public void update(String brand, String name, Money regularPrice, Money salePrice, LocalDateTime syncedAt) {
        this.brand = brand;
        this.name = name;
        this.regularPrice = regularPrice;
        this.salePrice = salePrice;
        this.syncedAt = syncedAt;
    }

    public UUID getId() { return id; }
    public String getGoodsNo() { return goodsNo; }
    public String getBrand() { return brand; }
    public String getName() { return name; }
    public Money getRegularPrice() { return regularPrice; }
    public Money getSalePrice() { return salePrice; }
    public LocalDateTime getSyncedAt() { return syncedAt; }
}
