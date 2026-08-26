package com.oliveyoung.mate.application.crew;

import com.oliveyoung.mate.application.crew.command.SignUpCommand;
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
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signUp(SignUpCommand cmd) {
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
    }
}
