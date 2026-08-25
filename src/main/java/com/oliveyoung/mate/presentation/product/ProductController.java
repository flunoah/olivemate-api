package com.oliveyoung.mate.presentation.product;

import com.oliveyoung.mate.application.product.ProductSearchService;
import com.oliveyoung.mate.application.product.result.ProductSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductSearchService productSearchService;

    // 상품명 자동완성. 인증만 되면 누구나 조회 가능 (SecurityConfig의 anyRequest().authenticated())
    @GetMapping("/search")
    public ResponseEntity<List<ProductSearchResult>> search(@RequestParam String q) {
        return ResponseEntity.ok(productSearchService.search(q));
    }
}
