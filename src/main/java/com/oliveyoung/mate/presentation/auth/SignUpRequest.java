package com.oliveyoung.mate.presentation.auth;

import com.oliveyoung.mate.domain.crew.model.Crew;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @NotBlank(message = "아이디를 입력해주세요.")
    String loginId,

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    String password,

    String name,
    Crew.Role role
) {}
