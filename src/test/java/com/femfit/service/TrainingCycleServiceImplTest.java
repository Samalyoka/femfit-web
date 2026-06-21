package com.femfit.service;

import com.femfit.dao.TrainingCycleDao;
import com.femfit.dto.TrainingCycleDto;
import com.femfit.model.TrainingCycle;
import com.femfit.service.impl.TrainingCycleServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TrainingCycleServiceImpl}.
 * Covers positive, negative, and boundary scenarios for cycle CRUD and
 * activation/deactivation (admin function).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrainingCycleServiceImpl tests")
class TrainingCycleServiceImplTest {

    @Mock private TrainingCycleDao cycleDao;
    @InjectMocks private TrainingCycleServiceImpl cycleService;

    // ── findAll / findAllActive ──────────────────────────────────────────

    @Test
    @DisplayName("findAll: returns all cycles regardless of active status")
    void findAll_returnsList() {
        List<TrainingCycle> cycles = List.of(
                TrainingCycle.builder().id(1).title("Yoga & Flexibility").active(true).build(),
                TrainingCycle.builder().id(2).title("Old Programme").active(false).build()
        );
        when(cycleDao.findAll()).thenReturn(cycles);

        List<TrainingCycle> result = cycleService.findAll();

        assertThat(result).hasSize(2);
        verify(cycleDao).findAll();
    }

    @Test
    @DisplayName("findAllActive: returns only active cycles")
    void findAllActive_returnsList() {
        List<TrainingCycle> cycles = List.of(
                TrainingCycle.builder().id(1).title("Yoga & Flexibility").active(true).build()
        );
        when(cycleDao.findAllActive()).thenReturn(cycles);

        List<TrainingCycle> result = cycleService.findAllActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("findAllActive: returns empty list when no cycles are active")
    void findAllActive_empty() {
        when(cycleDao.findAllActive()).thenReturn(Collections.emptyList());

        List<TrainingCycle> result = cycleService.findAllActive();

        assertThat(result).isEmpty();
    }

    // ── findAllActive (paginated) ───────────────────────────────────────

    @Test
    @DisplayName("findAllActive(paged): delegates offset/limit to DAO and returns the page")
    void findAllActivePaged_returnsPage() {
        List<TrainingCycle> page = List.of(
                TrainingCycle.builder().id(1).title("Yoga & Flexibility").active(true).build(),
                TrainingCycle.builder().id(2).title("Strength & Tone").active(true).build()
        );
        when(cycleDao.findAllActive(0, 9)).thenReturn(page);

        List<TrainingCycle> result = cycleService.findAllActive(0, 9);

        assertThat(result).hasSize(2);
        verify(cycleDao).findAllActive(0, 9);
    }

    @Test
    @DisplayName("findAllActive(paged): returns empty list for an out-of-range page")
    void findAllActivePaged_outOfRange() {
        when(cycleDao.findAllActive(100, 9)).thenReturn(Collections.emptyList());

        List<TrainingCycle> result = cycleService.findAllActive(100, 9);

        assertThat(result).isEmpty();
    }

    // ── findById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById: returns cycle when found")
    void findById_found() {
        TrainingCycle cycle = TrainingCycle.builder().id(7).title("Strength & Tone").build();
        when(cycleDao.findById(7)).thenReturn(Optional.of(cycle));

        Optional<TrainingCycle> result = cycleService.findById(7);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Strength & Tone");
    }

    @Test
    @DisplayName("findById: returns empty when not found")
    void findById_notFound() {
        when(cycleDao.findById(999)).thenReturn(Optional.empty());

        Optional<TrainingCycle> result = cycleService.findById(999);

        assertThat(result).isEmpty();
    }

    // ── create ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: builds a new cycle from DTO, active by default, and saves it")
    void create_success() {
        TrainingCycleDto dto = new TrainingCycleDto(
                "Beginner Fitness", "4-week full-body program for beginners.", 4, new BigDecimal("29900.00"));
        TrainingCycle saved = TrainingCycle.builder()
                .id(10).title("Beginner Fitness").durationWeeks(4)
                .price(new BigDecimal("29900.00")).active(true).build();
        when(cycleDao.save(any(TrainingCycle.class))).thenReturn(saved);

        TrainingCycle result = cycleService.create(dto);

        assertThat(result.getId()).isEqualTo(10);
        assertThat(result.isActive()).isTrue();

        ArgumentCaptor<TrainingCycle> captor = ArgumentCaptor.forClass(TrainingCycle.class);
        verify(cycleDao).save(captor.capture());
        TrainingCycle passed = captor.getValue();
        assertThat(passed.getTitle()).isEqualTo("Beginner Fitness");
        assertThat(passed.getDescription()).isEqualTo("4-week full-body program for beginners.");
        assertThat(passed.getDurationWeeks()).isEqualTo(4);
        assertThat(passed.getPrice()).isEqualTo(new BigDecimal("29900.00"));
        assertThat(passed.isActive()).isTrue();
    }

    @Test
    @DisplayName("create: boundary duration of 1 week (minimum) is passed through unchanged")
    void create_boundaryDurationMin() {
        TrainingCycleDto dto = new TrainingCycleDto("Quick Start", "One week intro.", 1, new BigDecimal("9900.00"));
        when(cycleDao.save(any(TrainingCycle.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingCycle result = cycleService.create(dto);

        assertThat(result.getDurationWeeks()).isEqualTo(1);
    }

    @Test
    @DisplayName("create: boundary duration of 52 weeks (maximum) is passed through unchanged")
    void create_boundaryDurationMax() {
        TrainingCycleDto dto = new TrainingCycleDto("Full Year Programme", "A full year of training.", 52, new BigDecimal("499900.00"));
        when(cycleDao.save(any(TrainingCycle.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingCycle result = cycleService.create(dto);

        assertThat(result.getDurationWeeks()).isEqualTo(52);
    }

    // ── update ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: applies DTO fields onto the existing cycle and saves it")
    void update_success() {
        TrainingCycle existing = TrainingCycle.builder()
                .id(5).title("Old Title").description("Old desc")
                .durationWeeks(6).price(new BigDecimal("39900.00")).active(true).build();
        when(cycleDao.findById(5)).thenReturn(Optional.of(existing));

        TrainingCycleDto dto = new TrainingCycleDto("New Title", "New desc", 8, new BigDecimal("44900.00"));
        cycleService.update(5, dto);

        ArgumentCaptor<TrainingCycle> captor = ArgumentCaptor.forClass(TrainingCycle.class);
        verify(cycleDao).update(captor.capture());
        TrainingCycle updated = captor.getValue();
        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getDescription()).isEqualTo("New desc");
        assertThat(updated.getDurationWeeks()).isEqualTo(8);
        assertThat(updated.getPrice()).isEqualTo(new BigDecimal("44900.00"));
        // active flag is untouched by update()
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    @DisplayName("update: throws RuntimeException when cycle does not exist")
    void update_notFound_throws() {
        when(cycleDao.findById(404)).thenReturn(Optional.empty());
        TrainingCycleDto dto = new TrainingCycleDto("Title", "Desc", 4, new BigDecimal("1000.00"));

        assertThatThrownBy(() -> cycleService.update(404, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("404");

        verify(cycleDao, never()).update(any());
    }

    // ── deactivate / activate ────────────────────────────────────────────

    @Test
    @DisplayName("deactivate: sets active flag to false via DAO")
    void deactivate_setsInactive() {
        cycleService.deactivate(3);
        verify(cycleDao).setActive(3, false);
    }

    @Test
    @DisplayName("activate: sets active flag to true via DAO")
    void activate_setsActive() {
        cycleService.activate(3);
        verify(cycleDao).setActive(3, true);
    }

    // ── countActive ──────────────────────────────────────────────────────

    @Test
    @DisplayName("countActive: returns active cycle count from DAO")
    void countActive_returnsCount() {
        when(cycleDao.countActive()).thenReturn(9);

        int result = cycleService.countActive();

        assertThat(result).isEqualTo(9);
    }

    @Test
    @DisplayName("countActive: returns 0 when no cycles are active")
    void countActive_zero() {
        when(cycleDao.countActive()).thenReturn(0);

        int result = cycleService.countActive();

        assertThat(result).isZero();
    }
}