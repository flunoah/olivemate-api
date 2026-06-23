package com.oliveyoung.mate.presentation.auth;

import com.oliveyoung.mate.application.crew.CrewService;
import com.oliveyoung.mate.application.crew.command.LoginCommand;
import com.oliveyoung.mate.application.crew.command.SignUpCommand;
import com.oliveyoung.mate.application.crew.result.TokenResult;
import com.oliveyoung.mate.presentation.ApiResponse;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CrewService crewService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResult>> register(@Valid @RequestBody SignUpRequest request) {
        TokenResult result = crewService.signUp(new SignUpCommand(
            request.email(),
            request.password(),
            request.name(),
            request.role()
        ));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResult>> login(@Valid @RequestBody LoginRequest request) {
        TokenResult result = crewService.login(new LoginCommand(
            request.email(),
            request.password()
        ));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResult>> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        TokenResult result = crewService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
