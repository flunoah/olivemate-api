package com.oliveyoung.mate.presentation.auth;

import com.oliveyoung.mate.domain.crew.model.Crew;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String email,

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    String password,

    String firstName,
    String lastName,
    Crew.Role role,
    String organizationId
) {
    public String name() {
        if (firstName == null && lastName == null) return email;
        String f = firstName != null ? firstName : "";
        String l = lastName  != null ? lastName  : "";
        return (f + " " + l).trim();
    }
}
