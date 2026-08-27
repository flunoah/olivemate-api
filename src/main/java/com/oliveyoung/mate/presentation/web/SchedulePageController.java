package com.oliveyoung.mate.presentation.web;

import com.oliveyoung.mate.application.point.PointService;
import com.oliveyoung.mate.application.schedule.ScheduleService;
import com.oliveyoung.mate.application.schedule.command.SaveScheduleCommand;
import com.oliveyoung.mate.application.schedule.result.SchedulePreviewResult;
import com.oliveyoung.mate.domain.crew.model.Crew;
import com.oliveyoung.mate.domain.crew.repository.CrewRepository;
import com.oliveyoung.mate.presentation.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class SchedulePageController {

    private static final String[] DAY_LABELS_SUN_FIRST = {"일", "월", "화", "수", "목", "금", "토"};

    private final ScheduleService scheduleService;
    private final CrewRepository crewRepository;
    private final PointService pointService;

    @GetMapping
    public String mypage(Model model) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        Crew crew = crewRepository.findById(crewId).orElseThrow();
        model.addAttribute("crewName", crew.getName());
        model.addAttribute("loginId", crew.getLoginId());
        var schedule = scheduleService.getMySchedule(crewId).orElse(null);
        model.addAttribute("schedule", schedule);
        model.addAttribute("scheduleDaysLabel", schedule == null ? null : schedule.daysOfWeek().stream()
            .sorted()
            .map(d -> DAY_LABELS_SUN_FIRST[d])
            .collect(java.util.stream.Collectors.joining(", ")));
        model.addAttribute("lifetimeStats", pointService.getLifetimeStats(crewId));
        return "mypage";
    }

    @PostMapping
    public String updateSchedule(
            @RequestParam List<Integer> daysOfWeek,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        scheduleService.saveSchedule(new SaveScheduleCommand(crewId, daysOfWeek, startDate, null));
        return "redirect:/mypage";
    }

    @GetMapping("/schedule/preview")
    public String previewSchedule(@RequestParam(required = false) List<Integer> daysOfWeek, Model model) {
        UUID crewId = SecurityUtils.authenticatedCrewId();
        SchedulePreviewResult preview = scheduleService.previewChange(
            crewId, daysOfWeek != null ? daysOfWeek : List.of());
        model.addAttribute("preview", preview);
        return "fragments/schedule-preview :: preview";
    }
}
