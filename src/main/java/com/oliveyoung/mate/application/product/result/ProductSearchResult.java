package com.oliveyoung.mate.application.product.result;

import java.util.UUID;

public record ProductSearchResult(UUID id, String goodsNo, String brand, String name, long regularPrice, long salePrice) {}
