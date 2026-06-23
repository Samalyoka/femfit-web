package com.femfit.controller;

import com.femfit.dto.PageDto;
import com.femfit.dto.TrainerProfileDto;
import com.femfit.service.TrainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Serves static informational pages about FemFit programmes and club features.
 * All pages are publicly accessible — no authentication required.
 *
 * Why Us pages (linked from home page cards):
 *   /about/trainers   — Personal trainers information
 *   /about/equipment  — Equipment and facilities
 *   /about/community  — Women's community
 *
 * Programme detail pages (linked from "What's included" section):
 *   /about/exercises      — Workout routines
 *   /about/nutrition      — Nutrition plan
 *   /about/schedule-info  — Training schedule
 */
@Controller
@RequestMapping("/about")
public class AboutController {

    /** Number of trainer cards shown per page on the "Our Trainers" gallery. */
    private static final int PAGE_SIZE_TRAINERS = 6;

    private final TrainerService trainerService;

    @Autowired
    public AboutController(TrainerService trainerService) {
        this.trainerService = trainerService;
    }

    /**
     * Personal trainers page.
     * Linked from the "Personal trainers" Why Us card on the home page.
     * Loads full public trainer profiles (photo, specialization, bio, rating)
     * so the page can render a real "meet the team" gallery, not just the
     * generic informational content.
     */
    @GetMapping("/trainers")
    public String trainers(@RequestParam(defaultValue = "1") int page, Model model) {
        PageDto<TrainerProfileDto> trainerPage = trainerService.getTrainerProfilesPage(page, PAGE_SIZE_TRAINERS);
        model.addAttribute("trainerPage", trainerPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", trainerPage.getTotalPages());
        return "about/trainers";
    }

    /**
     * Equipment and facilities page.
     * Linked from the "Premium equipment" Why Us card on the home page
     * and from the "Equipment Guide" programme section.
     */
    @GetMapping("/equipment")
    public String equipment() {
        return "about/equipment";
    }

    /**
     * Women's community page.
     * Linked from the "Women's community" Why Us card on the home page.
     */
    @GetMapping("/community")
    public String community() {
        return "about/community";
    }

    /**
     * Workout routines information page.
     * Linked from the "Workout Routines" programme section on the home page.
     */
    @GetMapping("/exercises")
    public String exercises() {
        return "about/exercises";
    }

    /**
     * Nutrition plan information page.
     * Linked from the "Nutrition Plan" programme section on the home page.
     */
    @GetMapping("/nutrition")
    public String nutrition() {
        return "about/nutrition";
    }

    /**
     * Training schedule information page.
     * Linked from the "Training Schedule" programme section on the home page.
     */
    @GetMapping("/schedule-info")
    public String scheduleInfo() {
        return "about/schedule-info";
    }
}