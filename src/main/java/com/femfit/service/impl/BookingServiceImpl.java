package com.femfit.service.impl;

import com.femfit.dao.BookingDao;
import com.femfit.dao.ClassScheduleDao;
import com.femfit.exception.BookingException;
import com.femfit.model.Booking;
import com.femfit.model.ClassSchedule;
import com.femfit.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of {@link BookingService}.
 * Validates capacity and duplicate bookings before persisting.
 */
@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingDao bookingDao;
    private final ClassScheduleDao scheduleDao;

    @Autowired
    public BookingServiceImpl(BookingDao bookingDao, ClassScheduleDao scheduleDao) {
        this.bookingDao = bookingDao;
        this.scheduleDao = scheduleDao;
    }

    @Override
    public Booking book(Long userId, Long scheduleId) {
        log.info("Booking request: userId={}, scheduleId={}", userId, scheduleId);

        // Check duplicate booking
        if (bookingDao.existsByUserAndSchedule(userId, scheduleId)) {
            throw new BookingException("You have already booked this class");
        }

        // Check capacity
        ClassSchedule schedule = scheduleDao.findById(scheduleId)
                .orElseThrow(() -> new BookingException("Class schedule not found"));

        int confirmed = bookingDao.countConfirmedByScheduleId(scheduleId);
        if (confirmed >= schedule.getCapacity()) {
            throw new BookingException("This class is fully booked");
        }

        Booking booking = Booking.builder()
                .userId(userId)
                .scheduleId(scheduleId)
                .status("CONFIRMED")
                .build();

        Booking saved = bookingDao.save(booking);
        log.info("Booking created: id={}", saved.getId());
        return saved;
    }

    @Override
    public void cancel(Long bookingId, Long userId) {
        log.info("Cancelling booking id={} by user id={}", bookingId, userId);
        bookingDao.cancel(bookingId, userId);
    }

    @Override
    public List<Booking> getUpcoming(Long userId) {
        return bookingDao.findUpcomingByUserId(userId);
    }

    @Override
    public int countVisitsThisMonth(Long userId) {
        return bookingDao.findUpcomingByUserId(userId).size();
    }
}