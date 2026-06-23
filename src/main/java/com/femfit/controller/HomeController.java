package com.femfit.controller;

import com.femfit.dao.ClassScheduleDao;
import com.femfit.model.ClassSchedule;
import com.femfit.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

/**
 * Handles public-facing pages: home, schedule, membership plans.
 */
@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    /** Number of recent reviews shown on the home page. */
    private static final int HOME_REVIEWS_COUNT = 3;

    private final ClassScheduleDao scheduleDao;
    private final ReviewService reviewService;

    @Autowired
    public HomeController(ClassScheduleDao scheduleDao, ReviewService reviewService) {
        this.scheduleDao = scheduleDao;
        this.reviewService = reviewService;
    }

    /**
     * Redirects root URL (/) to home page.
     * Ensures all root requests are properly handled.
     */
    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/home";
    }

    /**
     * Home page.
     * If the user was redirected here after a 403 (access denied),
     * reads the flashError from session and displays it once.
     * Always loads the 3 most recent reviews for public display.
     */
    @GetMapping("/home")
    public String home(Model model, HttpSession session, HttpServletRequest request) {
        Object flashError = session.getAttribute("flashError");
        if (flashError != null) {
            model.addAttribute("flashError", flashError);
            session.removeAttribute("flashError");
        }
        model.addAttribute("recentReviews", reviewService.getRecentReviews(HOME_REVIEWS_COUNT));
        return "home";
    }

    /**
     * Membership plans page.
     */
    @GetMapping("/plans")
    public String plans(Model model) {
        return "plans";
    }
}