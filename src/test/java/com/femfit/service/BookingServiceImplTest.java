package com.femfit.service;

import com.femfit.dao.BookingDao;
import com.femfit.exception.BookingException;
import com.femfit.model.Booking;
import com.femfit.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookingServiceImpl}.
 *
 * <p>As of the transactional refactor, all capacity/duplicate validation and
 * persistence for {@link BookingService#book} is performed atomically inside
 * {@link BookingDao#bookWithLock} (row-level lock + single transaction).
 * These tests therefore verify that the service correctly delegates to
 * {@code bookWithLock} and propagates its result/exceptions — the
 * lock/transaction/capacity logic itself lives in the DAO and is exercised
 * by integration tests against a real database, not by these mocked
 * unit tests.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingServiceImpl tests")
class BookingServiceImplTest {

    @Mock private BookingDao bookingDao;
    @InjectMocks private BookingServiceImpl bookingService;

    // ── book ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("book: delegates to bookingDao.bookWithLock and returns saved booking")
    void book_delegatesToBookWithLock() {
        Booking saved = Booking.builder().id(10L).memberId(1L).scheduleId(1L).status("CONFIRMED").build();
        when(bookingDao.bookWithLock(1L, 1L)).thenReturn(saved);

        Booking result = bookingService.book(1L, 1L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        verify(bookingDao).bookWithLock(1L, 1L);
    }

    @Test
    @DisplayName("book: throws BookingException when already booked")
    void book_alreadyBooked() {
        when(bookingDao.bookWithLock(1L, 1L))
                .thenThrow(new BookingException("You have already booked this class"));

        assertThatThrownBy(() -> bookingService.book(1L, 1L))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    @DisplayName("book: throws BookingException when class is full")
    void book_classFull() {
        when(bookingDao.bookWithLock(1L, 1L))
                .thenThrow(new BookingException("This class is fully booked"));

        assertThatThrownBy(() -> bookingService.book(1L, 1L))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("fully booked");
    }

    @Test
    @DisplayName("book: throws BookingException when schedule not found")
    void book_scheduleNotFound() {
        when(bookingDao.bookWithLock(1L, 99L))
                .thenThrow(new BookingException("Class schedule not found"));

        assertThatThrownBy(() -> bookingService.book(1L, 99L))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("not found");
    }

    // ── cancel ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancel: delegates to DAO")
    void cancel_success() {
        bookingService.cancel(5L, 1L);
        verify(bookingDao).cancel(5L, 1L);
    }

    // ── getUpcoming ──────────────────────────────────────────────────────

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

    @Test
    @DisplayName("getUpcoming: returns empty list when user has no bookings")
    void getUpcoming_empty() {
        when(bookingDao.findUpcomingByUserId(42L)).thenReturn(Collections.emptyList());

        List<Booking> result = bookingService.getUpcoming(42L);

        assertThat(result).isEmpty();
    }

    // ── countVisitsThisMonth ─────────────────────────────────────────────

    @Test
    @DisplayName("countVisitsThisMonth: returns size of upcoming bookings list")
    void countVisitsThisMonth_returnsCount() {
        List<Booking> bookings = List.of(
                Booking.builder().id(1L).className("Yoga").build(),
                Booking.builder().id(2L).className("Pilates").build(),
                Booking.builder().id(3L).className("Strength").build()
        );
        when(bookingDao.findUpcomingByUserId(1L)).thenReturn(bookings);

        int result = bookingService.countVisitsThisMonth(1L);

        assertThat(result).isEqualTo(3);
    }

    @Test
    @DisplayName("countVisitsThisMonth: returns 0 when user has no bookings")
    void countVisitsThisMonth_zero() {
        when(bookingDao.findUpcomingByUserId(42L)).thenReturn(Collections.emptyList());

        int result = bookingService.countVisitsThisMonth(42L);

        assertThat(result).isZero();
    }
}