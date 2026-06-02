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

    // ── 회원가입 ───────────────────────────────────
    @Transactional
    public void signUp(SignUpCommand cmd) {
        if (crewRepository.existsByLoginId(cmd.loginId())) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }
        Crew crew = Crew.create(
            cmd.loginId(),
            passwordEncoder.encode(cmd.password()),
            cmd.name()
        );
        crewRepository.save(crew);
    }

    // ── 로그인 ─────────────────────────────────────
    @Transactional(readOnly = true)
    public TokenResult login(LoginCommand cmd) {
        Crew crew = crewRepository.findByLoginId(cmd.loginId())
            .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다."));

        if (!passwordEncoder.matches(cmd.password(), crew.getPasswordHash())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다.");
        }

        String token = tokenProvider.generate(crew.getCrewId(), crew.getRole().name());
        return TokenResult.of(token, tokenProvider.expireSeconds());
    }
}