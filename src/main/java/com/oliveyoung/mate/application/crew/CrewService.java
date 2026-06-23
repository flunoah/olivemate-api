package com.oliveyoung.mate.application.crew;

import com.oliveyoung.mate.application.crew.command.LoginCommand;
import com.oliveyoung.mate.application.crew.command.SignUpCommand;
import com.oliveyoung.mate.application.crew.result.TokenResult;
import com.oliveyoung.mate.domain.crew.model.Crew;
import com.oliveyoung.mate.domain.crew.repository.CrewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrewService {

    private final CrewRepository  crewRepository;
    private final TokenProvider   tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenResult signUp(SignUpCommand cmd) {
        if (crewRepository.existsByLoginId(cmd.email())) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다.");
        }
        Crew crew = Crew.create(
            cmd.email(),
            passwordEncoder.encode(cmd.password()),
            cmd.name(),
            cmd.role() != null ? cmd.role() : Crew.Role.STUDENT
        );
        crewRepository.save(crew);

        String accessToken  = tokenProvider.generate(crew.getCrewId(), crew.getRole().name());
        String refreshToken = tokenProvider.generateRefresh(crew.getCrewId(), crew.getRole().name());
        return TokenResult.of(accessToken, refreshToken, tokenProvider.expireSeconds());
    }

    @Transactional(readOnly = true)
    public TokenResult login(LoginCommand cmd) {
        Crew crew = crewRepository.findByLoginId(cmd.email())
            .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 틀렸습니다."));

        if (!passwordEncoder.matches(cmd.password(), crew.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 틀렸습니다.");
        }

        String accessToken  = tokenProvider.generate(crew.getCrewId(), crew.getRole().name());
        String refreshToken = tokenProvider.generateRefresh(crew.getCrewId(), crew.getRole().name());
        return TokenResult.of(accessToken, refreshToken, tokenProvider.expireSeconds());
    }

    @Transactional(readOnly = true)
    public TokenResult refresh(String refreshToken) {
        com.oliveyoung.mate.infrastructure.crew.auth.JwtProvider jwt =
            (com.oliveyoung.mate.infrastructure.crew.auth.JwtProvider) tokenProvider;

        if (!jwt.validate(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        java.util.UUID crewId = jwt.extractCrewId(refreshToken);
        String role = jwt.extractRole(refreshToken);

        String newAccessToken  = tokenProvider.generate(crewId, role);
        String newRefreshToken = tokenProvider.generateRefresh(crewId, role);
        return TokenResult.of(newAccessToken, newRefreshToken, tokenProvider.expireSeconds());
    }
}
