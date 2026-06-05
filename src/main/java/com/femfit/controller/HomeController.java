package com.femfit.controller;

import com.femfit.dao.ClassScheduleDao;
import com.femfit.model.ClassSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles public-facing pages: home, schedule, membership plans.
 */
@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final ClassScheduleDao scheduleDao;

    @Autowired
    public HomeController(ClassScheduleDao scheduleDao) {
        this.scheduleDao = scheduleDao;
    }

    /**
     * Home page.
     */
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        return "home";
    }

    /**
     * Schedule page — shows upcoming classes, optionally filtered by category.
     *
     * @param category optional filter (YOGA, CARDIO, STRENGTH, PILATES, DANCE)
     * @param model    Spring MVC model
     * @return schedule view
     */
    @GetMapping("/schedule")
    public String schedule(@RequestParam(name = "category", required = false) String category, Model model) {
        log.info("Schedule page requested, category={}", category);
        try {
            List<ClassSchedule> schedules;
            if (category != null && !category.isBlank()) {
                schedules = scheduleDao.findUpcomingByCategory(category);
                model.addAttribute("activeCategory", category.toUpperCase());
            } else {
                schedules = scheduleDao.findUpcoming();
                model.addAttribute("activeCategory", "ALL");
            }
            log.info("Schedules found: {}", schedules.size());
            model.addAttribute("schedules", schedules);
            return "schedule";
        } catch (Exception e) {
            log.error("Error loading schedule: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Membership plans page.
     */
    @GetMapping("/plans")
    public String plans(Model model) {
        return "plans";
    }
}