package com.femfit.service.impl;

import com.femfit.dao.TrainingCycleDao;
import com.femfit.model.TrainingCycle;
import com.femfit.service.TrainingCycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link TrainingCycleService}.
 */
@Service
public class TrainingCycleServiceImpl implements TrainingCycleService {

    private final TrainingCycleDao trainingCycleDao;

    @Autowired
    public TrainingCycleServiceImpl(TrainingCycleDao trainingCycleDao) {
        this.trainingCycleDao = trainingCycleDao;
    }

    @Override
    public List<TrainingCycle> findAllActive() {
        return trainingCycleDao.findAllActive();
    }

    @Override
    public Optional<TrainingCycle> findById(Integer id) {
        return trainingCycleDao.findById(id);
    }
}