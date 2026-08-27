package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.product.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProductSearchPageController {

    private final ProductSearchService productSearchService;

    @GetMapping("/products/search")
    public String search(@RequestParam(required = false) String productName, Model model) {
        model.addAttribute("results", productSearchService.search(productName));
        model.addAttribute("query", productName);
        return "fragments/product-search-results :: results";
    }
}
