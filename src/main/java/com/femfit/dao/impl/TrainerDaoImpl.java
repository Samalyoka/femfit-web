package com.femfit.dao.impl;

import com.femfit.dao.TrainerDao;
import com.femfit.dto.ClientOrderDto;
import com.femfit.model.Role;
import com.femfit.model.User;
import com.femfit.util.pool.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TrainerDaoImpl implements TrainerDao {

    private static final Logger log = LoggerFactory.getLogger(TrainerDaoImpl.class);

    private final ConnectionPool pool;

    @Autowired
    public TrainerDaoImpl(ConnectionPool pool) {
        this.pool = pool;
    }

    // ─── SQL ──────────────────────────────────────────────────────────────────

    private static final String FIND_TRAINER_ID = """
            SELECT id FROM trainers WHERE user_id = ?
            """;

    private static final String FIND_CLIENTS = """
            SELECT DISTINCT u.id, u.first_name, u.last_name, u.email,
                            u.phone, u.enabled, r.name AS role
            FROM users u
            JOIN roles r  ON r.id  = u.role_id
            JOIN orders o ON o.user_id = u.id
            WHERE o.trainer_id = ?
              AND o.status IN ('ACTIVE', 'IN_PROGRESS')
            ORDER BY u.last_name, u.first_name
            """;

    /**
     * DISTINCT ON (u.id) — PostgreSQL-specific.
     * Берёт одну строку на клиента: самый свежий активный заказ.
     */
    private static final String FIND_CLIENTS_WITH_ORDER = """
            SELECT DISTINCT ON (u.id)
                   u.id          AS client_id,
                   u.first_name,
                   u.last_name,
                   u.email,
                   u.phone,
                   u.enabled,
                   o.id          AS order_id,
                   o.status      AS order_status
            FROM users u
            JOIN orders o ON o.user_id = u.id
            WHERE o.trainer_id = ?
              AND o.status IN ('ACTIVE', 'IN_PROGRESS')
            ORDER BY u.id, o.created_at DESC
            """;

    // ─── Implementations ──────────────────────────────────────────────────────

    @Override
    public long findTrainerIdByUserId(long userId) {
        try (Connection con = pool.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_TRAINER_ID)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
            throw new RuntimeException("Trainer record not found for userId=" + userId);
        } catch (SQLException e) {
            throw new RuntimeException("TrainerDao.findTrainerIdByUserId failed", e);
        }
    }

    @Override
    public List<User> findClientsByTrainerId(long trainerId) {
        List<User> clients = new ArrayList<>();
        try (Connection con = pool.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_CLIENTS)) {
            ps.setLong(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) clients.add(mapUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("TrainerDao.findClientsByTrainerId failed", e);
        }
        return clients;
    }

    @Override
    public List<ClientOrderDto> findClientsWithOrderByTrainerId(long trainerId) {
        List<ClientOrderDto> result = new ArrayList<>();
        try (Connection con = pool.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_CLIENTS_WITH_ORDER)) {
            ps.setLong(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapClientOrder(rs));
            }
        } catch (SQLException e) {
            log.error("TrainerDao.findClientsWithOrderByTrainerId failed: trainerId={}", trainerId, e);
            throw new RuntimeException("TrainerDao.findClientsWithOrderByTrainerId failed", e);
        }
        return result;
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setFirstName(rs.getString("first_name"));
        u.setLastName(rs.getString("last_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setEnabled(rs.getBoolean("enabled"));
        u.setRole(Role.valueOf(rs.getString("role")));
        return u;
    }

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