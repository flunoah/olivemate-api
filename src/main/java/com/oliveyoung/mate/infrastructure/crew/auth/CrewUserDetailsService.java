package com.oliveyoung.mate.infrastructure.crew.auth;

import com.oliveyoung.mate.domain.crew.repository.CrewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrewUserDetailsService implements UserDetailsService {

    private final CrewRepository crewRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) {
        return crewRepository.findByLoginId(loginId)
            .map(CrewPrincipal::fromCrew)
            .orElseThrow(() -> new UsernameNotFoundException("아이디 또는 비밀번호를 확인해주세요."));
    }
}
