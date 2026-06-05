package com.femfit.service;

import com.femfit.dao.BookingDao;
import com.femfit.dao.ClassScheduleDao;
import com.femfit.exception.BookingException;
import com.femfit.model.Booking;
import com.femfit.model.ClassSchedule;
import com.femfit.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookingServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingServiceImpl tests")
class BookingServiceImplTest {

    @Mock private BookingDao bookingDao;
    @Mock private ClassScheduleDao scheduleDao;
    @InjectMocks private BookingServiceImpl bookingService;

    private ClassSchedule schedule;

    @BeforeEach
    void setUp() {
        schedule = ClassSchedule.builder()
                .id(1L).capacity(15).className("Morning Yoga").build();
    }

    @Test
    @DisplayName("book: success when slot available and not already booked")
    void book_success() {
        when(bookingDao.existsByUserAndSchedule(1L, 1L)).thenReturn(false);
        when(scheduleDao.findById(1L)).thenReturn(Optional.of(schedule));
        when(bookingDao.countConfirmedByScheduleId(1L)).thenReturn(5);
        Booking saved = Booking.builder().id(10L).userId(1L).scheduleId(1L).status("CONFIRMED").build();
        when(bookingDao.save(any())).thenReturn(saved);

        Booking result = bookingService.book(1L, 1L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        verify(bookingDao).save(any(Booking.class));
    }

    @Test
    @DisplayName("book: throws BookingException when already booked")
    void book_alreadyBooked() {
        when(bookingDao.existsByUserAndSchedule(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> bookingService.book(1L, 1L))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("already booked");

        verify(bookingDao, never()).save(any());
    }

    @Test
    @DisplayName("book: throws BookingException when class is full")
    void book_classFull() {
        when(bookingDao.existsByUserAndSchedule(1L, 1L)).thenReturn(false);
        when(scheduleDao.findById(1L)).thenReturn(Optional.of(schedule));
        when(bookingDao.countConfirmedByScheduleId(1L)).thenReturn(15); // at capacity

        assertThatThrownBy(() -> bookingService.book(1L, 1L))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("fully booked");

        verify(bookingDao, never()).save(any());
    }

    @Test
    @DisplayName("book: throws BookingException when schedule not found")
    void book_scheduleNotFound() {
        when(bookingDao.existsByUserAndSchedule(1L, 99L)).thenReturn(false);
        when(scheduleDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.book(1L, 99L))
                .isInstanceOf(BookingException.class);
    }

    @Test
    @DisplayName("cancel: delegates to DAO")
    void cancel_success() {
        bookingService.cancel(5L, 1L);
        verify(bookingDao).cancel(5L, 1L);
    }

    @Test
    @DisplayName("getUpcoming: returns list from DAO")
    void getUpcoming_returnsList() {
        List<Booking> bookings = List.of(
                Booking.builder().id(1L).className("Yoga").build(),
                Booking.builder().id(2L).className("Pilates").build()
        );
        when(bookingDao.findUpcomingByUserId(1L)).thenReturn(bookings);

        List<Booking> result = bookingService.getUpcoming(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getClassName()).isEqualTo("Yoga");
    }
}