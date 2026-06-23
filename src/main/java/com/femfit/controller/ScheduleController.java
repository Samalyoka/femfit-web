package com.femfit.controller;

import com.femfit.dao.BookingDao;
import com.femfit.dao.ClassScheduleDao;
import com.femfit.model.ClassSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * Controller responsible for handling fitness class schedules,
 * filtering by categories, and managing user authentication state for bookings.
 *
 * Shows which classes the current user has already booked.
 */
@Controller
public class ScheduleController {

    private static final Logger log = LoggerFactory.getLogger(ScheduleController.class);

    /** Number of class schedule rows shown per page on the schedule page. */
    private static final int PAGE_SIZE_SCHEDULE = 10;

    private final ClassScheduleDao scheduleDao;
    private final BookingDao bookingDao;

    @Autowired
    public ScheduleController(ClassScheduleDao scheduleDao, BookingDao bookingDao) {
        this.scheduleDao = scheduleDao;
        this.bookingDao = bookingDao;
    }

    /**
     * Displays the fitness class schedule page.
     * Integrates optional category filtering and checks if the current user is authorized.
     * Shows which classes the user has already booked.
     *
     * @param category  optional filter parameter (e.g., YOGA, CARDIO, STRENGTH, PILATES, DANCE)
     * @param model     Spring MVC model to pass attributes to the Thymeleaf view
     * @param principal represents the currently authenticated user
     * @return the name of the schedule view template
     */
    @GetMapping("/schedule")
    public String showSchedule(@RequestParam(name = "category", required = false) String category,
                               @RequestParam(defaultValue = "1") int page,
                               Model model,
                               Principal principal) {
        log.info("Schedule page requested. Filter category: {}, page: {}, user: {}",
                category, page, (principal != null ? principal.getName() : "GUEST"));

        List<ClassSchedule> schedules;
        int total;
        int offset = (page - 1) * PAGE_SIZE_SCHEDULE;

        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
            String cat = category.toUpperCase();
            schedules = scheduleDao.findUpcomingByCategory(cat, offset, PAGE_SIZE_SCHEDULE);
            total = scheduleDao.countUpcomingByCategory(cat);
            model.addAttribute("activeCategory", cat);
        } else {
            schedules = scheduleDao.findUpcoming(offset, PAGE_SIZE_SCHEDULE);
            total = scheduleDao.countUpcoming();
            model.addAttribute("activeCategory", "ALL");
        }

        int totalPages = (int) Math.ceil(total / (double) PAGE_SIZE_SCHEDULE);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        boolean isAuthenticated = (principal != null);
        model.addAttribute("isAuthenticated", isAuthenticated);

        // Fetch user's booked schedule IDs if authenticated
        List<Long> bookedIds;
        if (isAuthenticated) {
            bookedIds = bookingDao.findBookedScheduleIdsByEmail(principal.getName());
            log.debug("User {} has booked {} classes", principal.getName(), bookedIds.size());
        } else {
            bookedIds = Collections.emptyList();
        }
        model.addAttribute("bookedScheduleIds", bookedIds);

        model.addAttribute("schedules", schedules);
        return "schedule";
    }
}