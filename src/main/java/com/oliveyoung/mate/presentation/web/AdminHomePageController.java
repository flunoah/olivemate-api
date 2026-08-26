package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.domain.crew.model.Crew;
import com.oliveyoung.mate.domain.crew.repository.CrewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomePageController {

    private final CrewRepository crewRepository;

    @GetMapping
    public String adminHome(Model model) {
        model.addAttribute("crews",
            crewRepository.findAllActiveByRole(Crew.Role.CREW).stream()
                .map(c -> new CrewView(c.getCrewId(), c.getName(), c.getLoginId()))
                .toList());
        return "admin";
    }

    private record CrewView(UUID crewId, String name, String loginId) {}
}
