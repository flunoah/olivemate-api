package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.crew.CrewService;
import com.oliveyoung.mate.application.crew.command.SignUpCommand;
import com.oliveyoung.mate.domain.crew.model.Crew;
import com.oliveyoung.mate.domain.crew.repository.CrewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomePageController {

    private final CrewRepository crewRepository;
    private final CrewService crewService;

    @GetMapping
    public String adminHome(Model model) {
        model.addAttribute("crews",
            crewRepository.findAllActiveByRole(Crew.Role.CREW).stream()
                .map(c -> new CrewView(c.getCrewId(), c.getName(), c.getLoginId()))
                .toList());
        return "admin";
    }

    @GetMapping("/crews/new")
    public String newCrewForm() {
        return "admin-crew-new";
    }

    @PostMapping("/crews/new")
    public String registerCrew(
            @RequestParam String loginId,
            @RequestParam String password,
            @RequestParam String name,
            Model model) {
        try {
            crewService.signUp(new SignUpCommand(loginId, password, name, null));
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "admin-crew-new";
        }
        UUID crewId = crewRepository.findByLoginId(loginId).orElseThrow().getCrewId();
        return "redirect:/admin/crews/" + crewId;
    }

    private record CrewView(UUID crewId, String name, String loginId) {}
}
