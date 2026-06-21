package com.femfit.service.impl;

import com.femfit.dao.TrainingCycleDao;
import com.femfit.dto.TrainingCycleDto;
import com.femfit.model.TrainingCycle;
import com.femfit.service.TrainingCycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link TrainingCycleService}.
 */
@Service
public class TrainingCycleServiceImpl implements TrainingCycleService {

    private static final Logger log = LoggerFactory.getLogger(TrainingCycleServiceImpl.class);

    private final TrainingCycleDao cycleDao;

    @Autowired
    public TrainingCycleServiceImpl(TrainingCycleDao cycleDao) {
        this.cycleDao = cycleDao;
    }

    @Override
    public List<TrainingCycle> findAll() {
        return cycleDao.findAll();
    }

    @Override
    public List<TrainingCycle> findAllActive() {
        return cycleDao.findAllActive();
    }

    @Override
    public List<TrainingCycle> findAllActive(int offset, int limit) {
        return cycleDao.findAllActive(offset, limit);
    }

    @Override
    public Optional<TrainingCycle> findById(Integer id) {
        return cycleDao.findById(id);
    }

    /**
     * Creates a new active training cycle from admin form DTO.
     */
    @Override
    public TrainingCycle create(TrainingCycleDto dto) {
        TrainingCycle cycle = TrainingCycle.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .durationWeeks(dto.getDurationWeeks())
                .price(dto.getPrice())
                .active(true)
                .build();
        TrainingCycle saved = cycleDao.save(cycle);
        log.info("Training cycle created: id={}, title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    /**
     * Updates title, description, duration and price of an existing cycle.
     */
    @Override
    public void update(Integer id, TrainingCycleDto dto) {
        TrainingCycle cycle = cycleDao.findById(id)
                .orElseThrow(() -> new RuntimeException("Training cycle not found: " + id));
        cycle.setTitle(dto.getTitle());
        cycle.setDescription(dto.getDescription());
        cycle.setDurationWeeks(dto.getDurationWeeks());
        cycle.setPrice(dto.getPrice());
        cycleDao.update(cycle);
        log.info("Training cycle updated: id={}", id);
    }

    /**
     * Deactivates a cycle — it will no longer be visible to clients.
     */
    @Override
    public void deactivate(Integer id) {
        cycleDao.setActive(id, false);
        log.info("Training cycle deactivated: id={}", id);
    }

    /**
     * Reactivates a previously deactivated cycle.
     */
    @Override
    public void activate(Integer id) {
        cycleDao.setActive(id, true);
        log.info("Training cycle activated: id={}", id);
    }

    @Override
    public int countActive() {
        return cycleDao.countActive();
    }
}