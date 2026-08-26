package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.product.ProductSyncService;
import com.oliveyoung.mate.application.product.ProductUploadException;
import com.oliveyoung.mate.application.product.ProductUploadItem;
import com.oliveyoung.mate.application.product.result.ProductUploadResult;
import com.oliveyoung.mate.infrastructure.product.excel.ProductExcelParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductPageController {

    private final ProductExcelParser productExcelParser;
    private final ProductSyncService productSyncService;

    @GetMapping
    public String adminProducts() {
        return "admin-products";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            List<ProductUploadItem> items = productExcelParser.parse(file);
            int synced = productSyncService.syncAll(items);
            redirectAttributes.addFlashAttribute("result",
                new ProductUploadResult(items.size(), synced, items.size() - synced));
        } catch (ProductUploadException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products";
    }
}
