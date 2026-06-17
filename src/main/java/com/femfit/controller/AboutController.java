package com.femfit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    /**
     * Personal trainers page.
     * Linked from the "Personal trainers" Why Us card on the home page.
     */
    @GetMapping("/trainers")
    public String trainers() {
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