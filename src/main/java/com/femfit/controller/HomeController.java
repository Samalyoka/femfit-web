package com.femfit.controller;

import com.femfit.dao.ClassScheduleDao;
import com.femfit.model.ClassSchedule;
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

    private final ClassScheduleDao scheduleDao;

    @Autowired
    public HomeController(ClassScheduleDao scheduleDao) {
        this.scheduleDao = scheduleDao;
    }

    /**
     * Home page.
     * If the user was redirected here after a 403 (access denied),
     * reads the flashError from session and displays it once.
     */
    @GetMapping("/home")
    public String home(Model model, HttpSession session, HttpServletRequest request) {
        // Получаем flashError из session
        Object flashError = session.getAttribute("flashError");
        if (flashError != null) {
            model.addAttribute("flashError", flashError);
            // Удаляем сразу после прочтения
            session.removeAttribute("flashError");
        }
        return "home";
    }

    /**
     * Membership plans page.
     */
    @GetMapping("/plans")
    public String plans(Model model) {
        return "plans";
    }

    /**
     * Error page for access denied (403).
     */
    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }

}