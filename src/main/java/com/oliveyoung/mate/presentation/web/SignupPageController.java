package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.crew.CrewService;
import com.oliveyoung.mate.application.crew.command.SignUpCommand;
import com.oliveyoung.mate.application.point.PointService;
import com.oliveyoung.mate.application.point.command.InitPointCommand;
import com.oliveyoung.mate.application.schedule.ScheduleService;
import com.oliveyoung.mate.application.schedule.command.SaveScheduleCommand;
import com.oliveyoung.mate.domain.crew.repository.CrewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class SignupPageController {

    private final CrewService crewService;
    private final PointService pointService;
    private final ScheduleService scheduleService;
    private final CrewRepository crewRepository;

    @GetMapping("/signup")
    public String signupForm() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(
            @RequestParam String loginId,
            @RequestParam String password,
            @RequestParam String name,
            @RequestParam(required = false) Long initialPoints,
            @RequestParam List<Integer> daysOfWeek,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            Model model) {
        try {
            crewService.signUp(new SignUpCommand(loginId, password, name, null));
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "signup";
        }
        UUID crewId = crewRepository.findByLoginId(loginId).orElseThrow().getCrewId();

        if (initialPoints != null && initialPoints > 0) {
            pointService.initialize(new InitPointCommand(crewId, initialPoints));
        }
        scheduleService.saveSchedule(new SaveScheduleCommand(crewId, daysOfWeek, startDate, null));

        model.addAttribute("done", true);
        model.addAttribute("doneInitialPoints", initialPoints == null ? 0L : initialPoints);
        return "signup";
    }
}
