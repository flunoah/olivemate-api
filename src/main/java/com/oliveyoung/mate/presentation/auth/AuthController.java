package com.oliveyoung.mate.presentation.auth;

import com.oliveyoung.mate.application.crew.CrewService;
import com.oliveyoung.mate.application.crew.command.LoginCommand;
import com.oliveyoung.mate.application.crew.command.SignUpCommand;
import com.oliveyoung.mate.application.crew.result.TokenResult;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CrewService crewService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequest request) {
        crewService.signUp(new SignUpCommand(
            request.loginId(),
            request.password(),
            request.name()
        ));
        return ResponseEntity.ok("회원가입 완료!");
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenResult> login(@Valid @RequestBody LoginRequest request) {
        TokenResult result = crewService.login(new LoginCommand(
            request.loginId(),
            request.password()
        ));
        return ResponseEntity.ok(result);
    }
}