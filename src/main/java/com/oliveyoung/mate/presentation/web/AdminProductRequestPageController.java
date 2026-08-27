package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.productrequest.ProductRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/product-requests")
@RequiredArgsConstructor
public class AdminProductRequestPageController {

    private final ProductRequestService productRequestService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("requests", productRequestService.getPending());
        return "admin-product-requests";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            productRequestService.approve(id);
            redirectAttributes.addFlashAttribute("message", "승인했어요.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/product-requests";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        productRequestService.reject(id);
        redirectAttributes.addFlashAttribute("message", "반려했어요.");
        return "redirect:/admin/product-requests";
    }
}
