package com.femfit.service;

import com.femfit.dao.AssignmentDao;
import com.femfit.dao.TrainerDao;
import com.femfit.dto.ClientOrderDto;
import com.femfit.dto.TrainerDto;
import com.femfit.model.Assignment;
import com.femfit.model.Member;
import com.femfit.service.impl.TrainerServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TrainerServiceImpl}.
 * Covers positive, negative, and edge-case scenarios for trainer dashboard
 * and trainer-selection (DTO projection) flows.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrainerServiceImpl tests")
class TrainerServiceImplTest {

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private AssignmentDao assignmentDao;

    @InjectMocks
    private TrainerServiceImpl trainerService;

    // ── getAllTrainers (DTO projection) ──────────────────────────────────

    @Test
    @DisplayName("getAllTrainers: returns DTO list from DAO")
    void getAllTrainers_returnsDtoList() {
        List<TrainerDto> trainers = List.of(
                TrainerDto.builder().id(18L).firstName("Elena").lastName("Morozova").email("elena@femfit.kz").build(),
                TrainerDto.builder().id(20L).firstName("Sofia").lastName("Romanova").email("sofia@femfit.kz").build()
        );
        when(trainerDao.findAllTrainers()).thenReturn(trainers);

        List<TrainerDto> result = trainerService.getAllTrainers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFullName()).isEqualTo("Elena Morozova");
        assertThat(result.get(0).getEmail()).isEqualTo("elena@femfit.kz");
    }

    @Test
    @DisplayName("getAllTrainers: returns empty list when no active trainers")
    void getAllTrainers_empty() {
        when(trainerDao.findAllTrainers()).thenReturn(Collections.emptyList());

        List<TrainerDto> result = trainerService.getAllTrainers();

        assertThat(result).isEmpty();
    }

    // ── getClientsByTrainerUserId ─────────────────────────────────────────

    @Test
    @DisplayName("getClientsByTrainerUserId: resolves trainerId then returns clients")
    void getClientsByTrainerUserId_success() {
        when(trainerDao.findTrainerIdByUserId(18L)).thenReturn(18L);
        List<Member> clients = List.of(Member.builder().id(1L).firstName("Anna").build());
        when(trainerDao.findClientsByTrainerId(18L)).thenReturn(clients);

        List<Member> result = trainerService.getClientsByTrainerUserId(18L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Anna");
        verify(trainerDao).findTrainerIdByUserId(18L);
        verify(trainerDao).findClientsByTrainerId(18L);
    }

    @Test
    @DisplayName("getClientsByTrainerUserId: returns empty list when trainer has no clients")
    void getClientsByTrainerUserId_noClients() {
        when(trainerDao.findTrainerIdByUserId(20L)).thenReturn(20L);
        when(trainerDao.findClientsByTrainerId(20L)).thenReturn(Collections.emptyList());

        List<Member> result = trainerService.getClientsByTrainerUserId(20L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getClientsByTrainerUserId: propagates exception when trainer record not found")
    void getClientsByTrainerUserId_trainerNotFound() {
        when(trainerDao.findTrainerIdByUserId(999L))
                .thenThrow(new RuntimeException("Trainer record not found for userId=999"));

        assertThatThrownBy(() -> trainerService.getClientsByTrainerUserId(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");

        verify(trainerDao, never()).findClientsByTrainerId(anyLong());
    }

    // ── getClientsWithOrderByTrainerUserId ────────────────────────────────

    @Test
    @DisplayName("getClientsWithOrderByTrainerUserId: returns DTO list with order info")
    void getClientsWithOrder_success() {
        when(trainerDao.findTrainerIdByUserId(18L)).thenReturn(18L);
        List<ClientOrderDto> dtos = List.of(
                ClientOrderDto.builder().clientId(1L).firstName("Anna").lastName("Kim")
                        .orderId(5L).orderStatus("ACTIVE").build()
        );
        when(trainerDao.findClientsWithOrderByTrainerId(18L)).thenReturn(dtos);

        List<ClientOrderDto> result = trainerService.getClientsWithOrderByTrainerUserId(18L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("Anna Kim");
        assertThat(result.get(0).getOrderStatus()).isEqualTo("ACTIVE");
    }

    // ── getAssignmentForClient ─────────────────────────────────────────────

    @Test
    @DisplayName("getAssignmentForClient: returns assignment when present")
    void getAssignmentForClient_found() {
        Assignment assignment = Assignment.builder().id(1L).orderId(5L).status("ACTIVE").build();
        when(assignmentDao.findLatestByClientId(1L)).thenReturn(Optional.of(assignment));

        Optional<Assignment> result = trainerService.getAssignmentForClient(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getAssignmentForClient: returns empty when no assignment exists")
    void getAssignmentForClient_notFound() {
        when(assignmentDao.findLatestByClientId(99L)).thenReturn(Optional.empty());

        Optional<Assignment> result = trainerService.getAssignmentForClient(99L);

        assertThat(result).isEmpty();
    }

    // ── saveOrUpdateAssignment ─────────────────────────────────────────────

    @Test
    @DisplayName("saveOrUpdateAssignment: creates new assignment when none exists for order")
    void saveOrUpdateAssignment_creates() {
        Assignment assignment = Assignment.builder().orderId(5L).exercises("Squats").build();
        when(assignmentDao.findByOrderId(5L)).thenReturn(Optional.empty());

        trainerService.saveOrUpdateAssignment(assignment);

        verify(assignmentDao).save(assignment);
        verify(assignmentDao, never()).update(any());
    }

    @Test
    @DisplayName("saveOrUpdateAssignment: updates existing assignment, preserving its id")
    void saveOrUpdateAssignment_updates() {
        Assignment existing = Assignment.builder().id(7L).orderId(5L).exercises("Old plan").build();
        Assignment incoming = Assignment.builder().orderId(5L).exercises("New plan").build();
        when(assignmentDao.findByOrderId(5L)).thenReturn(Optional.of(existing));

        trainerService.saveOrUpdateAssignment(incoming);

        assertThat(incoming.getId()).isEqualTo(7L);
        verify(assignmentDao).update(incoming);
        verify(assignmentDao, never()).save(any());
    }

    // ── deleteAssignment / updateAssignmentStatus ──────────────────────────

    @Test
    @DisplayName("deleteAssignment: delegates to DAO")
    void deleteAssignment_delegates() {
        trainerService.deleteAssignment(5L);
        verify(assignmentDao).deleteByOrderId(5L);
    }

    @Test
    @DisplayName("updateAssignmentStatus: delegates to DAO")
    void updateAssignmentStatus_delegates() {
        trainerService.updateAssignmentStatus(7L, "COMPLETED");
        verify(assignmentDao).updateStatus(7L, "COMPLETED");
    }
}