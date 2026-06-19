package com.femfit.dao.impl;

import com.femfit.dao.TrainerDao;
import com.femfit.dto.ClientOrderDto;
import com.femfit.dto.TrainerDto;
import com.femfit.model.Member;
import com.femfit.model.Role;
import com.femfit.datasource.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of {@link TrainerDao}.
 *
 * Provides trainer-specific data access including:
 * - Finding clients assigned to a trainer
 * - Retrieving client-order relationships
 * - Listing all active trainers
 *
 * Uses PostgreSQL-specific DISTINCT ON for efficient client listing.
 */
@Repository
public class TrainerDaoImpl implements TrainerDao {

    private static final Logger log = LoggerFactory.getLogger(TrainerDaoImpl.class);

    private final ConnectionPool pool;

    @Autowired
    public TrainerDaoImpl(ConnectionPool pool) {
        this.pool = pool;
    }

    private static final String FIND_TRAINER_ID = """
            SELECT id FROM trainers WHERE id = ?
            """;

    private static final String FIND_CLIENTS = """
            SELECT DISTINCT u.id, u.first_name, u.last_name, u.email,
                            u.phone, u.is_active AS enabled, r.name AS role
            FROM members u
            JOIN roles r  ON r.id  = u.role_id
            JOIN orders o ON o.member_id = u.id
            WHERE o.trainer_id = ?
              AND o.status IN ('ACTIVE', 'IN_PROGRESS')
            ORDER BY u.last_name, u.first_name
            """;

    /**
     * DISTINCT ON (u.id) — PostgreSQL-specific.
     * Returns one row per client, with their most recent active/in-progress order if exists,
     * otherwise nulls for order fields.
     */
    private static final String FIND_CLIENTS_WITH_ORDER = """
            SELECT DISTINCT ON (u.id)
                   u.id          AS client_id,
                   u.first_name,
                   u.last_name,
                   u.email,
                   u.phone,
                   u.is_active AS enabled,
                   o.id          AS order_id,
                   o.status      AS order_status
            FROM members u
            JOIN orders o ON o.member_id = u.id
            WHERE o.trainer_id = ?
              AND o.status IN ('ACTIVE', 'IN_PROGRESS')
            ORDER BY u.id, o.created_at DESC
            """;

    /**
     * Lightweight projection — only fields needed for trainer-selection UI.
     */
    private static final String FIND_ALL_TRAINERS = """
            SELECT u.id, u.first_name, u.last_name, u.email
            FROM members u
            JOIN roles r ON r.id = u.role_id
            WHERE r.name = 'TRAINER'
              AND u.is_active = true
            ORDER BY u.first_name
            """;

    /**
     * Finds the trainer ID for a given user ID.
     * Used to resolve the trainer record from the user/member record.
     *
     * @param userId the user ID (must correspond to a trainer)
     * @return the trainer ID
     * @throws RuntimeException if the trainer record is not found
     */
    @Override
    public long findTrainerIdByUserId(long userId) {
        Connection con = pool.getConnection();
        try (PreparedStatement ps = con.prepareStatement(FIND_TRAINER_ID)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
            throw new RuntimeException("Trainer record not found for userId=" + userId);
        } catch (SQLException e) {
            throw new RuntimeException("TrainerDao.findTrainerIdByUserId failed", e);
        } finally {
            pool.releaseConnection(con);
        }
    }

    /**
     * Finds all clients assigned to a trainer with active or in-progress orders.
     * Results are ordered by last name, then first name.
     *
     * @param trainerId the trainer ID
     * @return list of members who have orders with this trainer, empty list if none found
     * @throws RuntimeException if the query fails
     */
    @Override
    public List<Member> findClientsByTrainerId(long trainerId) {
        List<Member> clients = new ArrayList<>();
        Connection con = pool.getConnection();
        try (PreparedStatement ps = con.prepareStatement(FIND_CLIENTS)) {
            ps.setLong(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) clients.add(mapUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("TrainerDao.findClientsByTrainerId failed", e);
        } finally {
            pool.releaseConnection(con);
        }
        return clients;
    }

    /**
     * Finds all clients assigned to a trainer, including their most recent order details.
     * Uses PostgreSQL DISTINCT ON to return one row per client with latest order.
     * Results are ordered by client ID.
     *
     * @param trainerId the trainer ID
     * @return list of client-order DTOs, empty list if none found
     * @throws RuntimeException if the query fails
     */
    @Override
    public List<ClientOrderDto> findClientsWithOrderByTrainerId(long trainerId) {
        List<ClientOrderDto> result = new ArrayList<>();
        Connection con = pool.getConnection();
        try (PreparedStatement ps = con.prepareStatement(FIND_CLIENTS_WITH_ORDER)) {
            ps.setLong(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapClientOrder(rs));
            }
        } catch (SQLException e) {
            log.error("TrainerDao.findClientsWithOrderByTrainerId failed: trainerId={}", trainerId, e);
            throw new RuntimeException("TrainerDao.findClientsWithOrderByTrainerId failed", e);
        } finally {
            pool.releaseConnection(con);
        }
        return result;
    }

    /**
     * Finds all active trainers in the system.
     * Returns lightweight DTO with only essential fields.
     * Results are ordered by first name.
     *
     * @return list of all active trainers, empty list if none found
     * @throws RuntimeException if the query fails
     */
    @Override
    public List<TrainerDto> findAllTrainers() {
        List<TrainerDto> trainers = new ArrayList<>();
        Connection con = pool.getConnection();
        try (PreparedStatement ps = con.prepareStatement(FIND_ALL_TRAINERS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) trainers.add(mapTrainer(rs));
        } catch (SQLException e) {
            throw new RuntimeException("TrainerDao.findAllTrainers failed", e);
        } finally {
            pool.releaseConnection(con);
        }
        return trainers;
    }

    /**
     * Maps a ResultSet row to a {@link Member} object.
     *
     * @param rs the result set positioned at the current row
     * @return a populated Member object
     * @throws SQLException if a column cannot be read
     */
    private Member mapUser(ResultSet rs) throws SQLException {
        Member u = new Member();
        u.setId(rs.getLong("id"));
        u.setFirstName(rs.getString("first_name"));
        u.setLastName(rs.getString("last_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setEnabled(rs.getBoolean("enabled"));
        u.setRole(Role.valueOf(rs.getString("role")));
        return u;
    }

    /**
     * Maps a ResultSet row to a {@link TrainerDto} object.
     *
     * @param rs the result set positioned at the current row
     * @return a populated TrainerDto object
     * @throws SQLException if a column cannot be read
     */
    private TrainerDto mapTrainer(ResultSet rs) throws SQLException {
        return TrainerDto.builder()
                .id(rs.getLong("id"))
                .firstName(rs.getString("first_name"))
                .lastName(rs.getString("last_name"))
                .email(rs.getString("email"))
                .build();
    }

    /**
     * Maps a ResultSet row to a {@link ClientOrderDto} object.
     *
     * @param rs the result set positioned at the current row
     * @return a populated ClientOrderDto object
     * @throws SQLException if a column cannot be read
     */
    private ClientOrderDto mapClientOrder(ResultSet rs) throws SQLException {
        return ClientOrderDto.builder()
                .clientId(rs.getLong("client_id"))
                .firstName(rs.getString("first_name"))
                .lastName(rs.getString("last_name"))
                .email(rs.getString("email"))
                .phone(rs.getString("phone"))
                .enabled(rs.getBoolean("enabled"))
                .orderId(rs.getLong("order_id"))
                .orderStatus(rs.getString("order_status"))
                .build();
    }
}