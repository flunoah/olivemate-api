package com.oliveyoung.mate.presentation.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthPageController {

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String registered,
            Model model) {
        if (error != null) {
            String message = "expired".equals(error)
                ? "세션이 만료되었습니다. 다시 로그인해주세요"
                : "아이디 또는 비밀번호를 확인해주세요";
            model.addAttribute("error", message);
        }
        if (logout != null) {
            model.addAttribute("message", "로그아웃되었습니다.");
        }
        if (registered != null) {
            model.addAttribute("message", "가입이 완료됐어요! 로그인해주세요.");
        }
        return "login";
    }
}
