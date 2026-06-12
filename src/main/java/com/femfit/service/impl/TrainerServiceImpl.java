package com.femfit.service.impl;

import com.femfit.dao.AssignmentDao;
import com.femfit.dao.TrainerDao;
import com.femfit.dto.ClientOrderDto;
import com.femfit.model.Assignment;
import com.femfit.model.Member;
import com.femfit.service.TrainerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TrainerServiceImpl implements TrainerService {

    private static final Logger log = LoggerFactory.getLogger(TrainerServiceImpl.class);

    private final TrainerDao trainerDao;
    private final AssignmentDao assignmentDao;

    @Autowired
    public TrainerServiceImpl(TrainerDao trainerDao, AssignmentDao assignmentDao) {
        this.trainerDao    = trainerDao;
        this.assignmentDao = assignmentDao;
    }

    @Override
    public List<Member> getClientsByTrainerUserId(long userId) {
        long trainerId = trainerDao.findTrainerIdByUserId(userId);
        return trainerDao.findClientsByTrainerId(trainerId);
    }

    @Override
    public List<ClientOrderDto> getClientsWithOrderByTrainerUserId(long userId) {
        long trainerId = trainerDao.findTrainerIdByUserId(userId);
        return trainerDao.findClientsWithOrderByTrainerId(trainerId);
    }

    @Override
    public Optional<Assignment> getAssignmentForClient(long clientId) {
        return assignmentDao.findLatestByClientId(clientId);
    }

    @Override
    public void saveOrUpdateAssignment(Assignment assignment) {
        Optional<Assignment> existing = assignmentDao.findByOrderId(assignment.getOrderId());
        if (existing.isPresent()) {
            assignment.setId(existing.get().getId());
            assignmentDao.update(assignment);
            log.debug("Assignment updated: orderId={}", assignment.getOrderId());
        } else {
            assignmentDao.save(assignment);
            log.debug("Assignment created: orderId={}", assignment.getOrderId());
        }
    }

    @Override
    public void deleteAssignment(long orderId) {
        assignmentDao.deleteByOrderId(orderId);
        log.info("Assignment deleted: orderId={}", orderId);
    }

    @Override
    public void updateAssignmentStatus(Long assignmentId, String status) {
        assignmentDao.updateStatus(assignmentId, status);
        log.debug("Assignment status set: id={}, status={}", assignmentId, status);
    }

    @Override
    public List<Member> getAllTrainers() {
        return trainerDao.findAllTrainers();
    }
}