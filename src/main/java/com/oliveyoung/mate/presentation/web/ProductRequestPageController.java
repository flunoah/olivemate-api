package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.productrequest.ProductRequestService;
import com.oliveyoung.mate.application.productrequest.command.SubmitProductRequestCommand;
import com.oliveyoung.mate.domain.productrequest.model.ProductRequest;
import com.oliveyoung.mate.presentation.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/products/requests")
@RequiredArgsConstructor
public class ProductRequestPageController {

    private final ProductRequestService productRequestService;

    @PostMapping
    public String submit(
            @RequestParam String requestType,
            @RequestParam String productName,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Long price,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) UUID linkedProductId,
            RedirectAttributes redirectAttributes) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        productRequestService.submit(new SubmitProductRequestCommand(
            crewId,
            ProductRequest.RequestType.valueOf(requestType),
            productName, brand, price, note, linkedProductId
        ));
        redirectAttributes.addFlashAttribute("message", "요청을 보냈어요. 관리자 확인 후 반영돼요.");
        return "redirect:/dashboard";
    }
}
