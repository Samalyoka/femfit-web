package com.femfit.dao;

import com.femfit.dao.impl.TrainingCycleDaoImpl;
import com.femfit.datasource.ConnectionPool;
import com.femfit.model.TrainingCycle;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TrainingCycleDaoImpl}.
 *
 * <p>JDBC dependencies ({@link ConnectionPool}, {@link Connection},
 * {@link PreparedStatement}, {@link ResultSet}) are fully mocked with Mockito,
 * so no database is required. Each test verifies that the DAO correctly maps
 * ResultSet rows to domain objects and handles edge cases (empty result,
 * SQL exceptions, nullable fields).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrainingCycleDaoImpl tests")
class TrainingCycleDaoImplTest {

    @Mock private ConnectionPool pool;
    @Mock private Connection conn;
    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;

    @InjectMocks private TrainingCycleDaoImpl dao;

    @BeforeEach
    void setUp() throws SQLException {
        when(pool.getConnection()).thenReturn(conn);
    }

    // ── findAll ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll: returns list of all cycles mapped from ResultSet")
    void findAll_returnsMappedList() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);

        mockCycleRow(rs, 1, "Yoga Basics", true);

        List<TrainingCycle> result = dao.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Yoga Basics");
        verify(pool).releaseConnection(conn);
    }

    @Test
    @DisplayName("findAll: returns empty list when no cycles exist")
    void findAll_empty_returnsEmptyList() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        List<TrainingCycle> result = dao.findAll();

        assertThat(result).isEmpty();
        verify(pool).releaseConnection(conn);
    }

    // ── findById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById: returns Optional with cycle when found")
    void findById_found_returnsOptional() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        mockCycleRow(rs, 5, "Strength Training", true);

        Optional<TrainingCycle> result = dao.findById(5);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(5);
        assertThat(result.get().getTitle()).isEqualTo("Strength Training");
        verify(ps).setInt(1, 5);
        verify(pool).releaseConnection(conn);
    }

    @Test
    @DisplayName("findById: returns empty Optional when cycle not found")
    void findById_notFound_returnsEmpty() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        Optional<TrainingCycle> result = dao.findById(99);

        assertThat(result).isEmpty();
        verify(pool).releaseConnection(conn);
    }

    // ── findAllActive ────────────────────────────────────────────────────

    @Test
    @DisplayName("findAllActive: returns only active cycles")
    void findAllActive_returnsActiveCycles() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);

        mockCycleRow(rs, 1, "Pilates", true);

        List<TrainingCycle> result = dao.findAllActive();

        assertThat(result).hasSize(2);
        verify(pool).releaseConnection(conn);
    }

    // ── findAllActive paged ───────────────────────────────────────────────

    @Test
    @DisplayName("findAllActive(offset, limit): sets LIMIT and OFFSET parameters")
    void findAllActivePaged_setsLimitAndOffset() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        dao.findAllActive(10, 5);

        verify(ps).setInt(1, 5);   // LIMIT
        verify(ps).setInt(2, 10);  // OFFSET
        verify(pool).releaseConnection(conn);
    }

    // ── countActive ──────────────────────────────────────────────────────

    @Test
    @DisplayName("countActive: returns count from ResultSet")
    void countActive_returnsCorrectCount() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(7);

        int count = dao.countActive();

        assertThat(count).isEqualTo(7);
        verify(pool).releaseConnection(conn);
    }

    @Test
    @DisplayName("countActive: returns 0 when ResultSet is empty")
    void countActive_emptyResultSet_returnsZero() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        int count = dao.countActive();

        assertThat(count).isZero();
        verify(pool).releaseConnection(conn);
    }

    // ── save ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save: sets id and createdAt from RETURNING clause")
    void save_setsGeneratedIdAndTimestamp() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("id")).thenReturn(42);
        Timestamp ts = Timestamp.valueOf("2026-01-01 10:00:00");
        when(rs.getTimestamp("created_at")).thenReturn(ts);

        TrainingCycle cycle = TrainingCycle.builder()
                .title("Dance Fit")
                .description("Fun cardio")
                .durationWeeks(4)
                .price(BigDecimal.valueOf(25000))
                .build();

        TrainingCycle saved = dao.save(cycle);

        assertThat(saved.getId()).isEqualTo(42);
        assertThat(saved.getCreatedAt()).isNotNull();
        verify(ps).setString(1, "Dance Fit");
        verify(ps).setString(2, "Fun cardio");
        verify(ps).setInt(3, 4);
        verify(ps).setBigDecimal(4, BigDecimal.valueOf(25000));
        verify(pool).releaseConnection(conn);
    }

    // ── setActive ────────────────────────────────────────────────────────

    @Test
    @DisplayName("setActive: sends correct parameters to PreparedStatement")
    void setActive_sendsCorrectParameters() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);

        dao.setActive(3, false);

        verify(ps).setBoolean(1, false);
        verify(ps).setInt(2, 3);
        verify(ps).executeUpdate();
        verify(pool).releaseConnection(conn);
    }

    // ── update ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: sends all fields to PreparedStatement")
    void update_sendsAllFields() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);

        TrainingCycle cycle = TrainingCycle.builder()
                .id(7)
                .title("Updated")
                .description("New desc")
                .durationWeeks(8)
                .price(BigDecimal.valueOf(30000))
                .build();

        dao.update(cycle);

        verify(ps).setString(1, "Updated");
        verify(ps).setString(2, "New desc");
        verify(ps).setInt(3, 8);
        verify(ps).setBigDecimal(4, BigDecimal.valueOf(30000));
        verify(ps).setInt(5, 7);
        verify(ps).executeUpdate();
        verify(pool).releaseConnection(conn);
    }

    // ── exception handling ───────────────────────────────────────────────

    @Test
    @DisplayName("findAll: wraps SQLException in RuntimeException")
    void findAll_sqlException_throwsRuntimeException() throws SQLException {
        when(conn.prepareStatement(any())).thenThrow(new SQLException("DB down"));

        assertThatThrownBy(() -> dao.findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed");

        verify(pool).releaseConnection(conn);
    }

    @Test
    @DisplayName("save: wraps SQLException in RuntimeException")
    void save_sqlException_throwsRuntimeException() throws SQLException {
        when(conn.prepareStatement(any())).thenThrow(new SQLException("constraint violation"));

        TrainingCycle cycle = TrainingCycle.builder()
                .title("Bad").description("x").durationWeeks(1)
                .price(BigDecimal.ONE).build();

        assertThatThrownBy(() -> dao.save(cycle))
                .isInstanceOf(RuntimeException.class);

        verify(pool).releaseConnection(conn);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void mockCycleRow(ResultSet rs, int id, String title, boolean active) throws SQLException {
        when(rs.getInt("id")).thenReturn(id);
        when(rs.getString("title")).thenReturn(title);
        when(rs.getString("description")).thenReturn("Some description");
        when(rs.getInt("duration_weeks")).thenReturn(6);
        when(rs.getBigDecimal("price")).thenReturn(BigDecimal.valueOf(20000));
        when(rs.getBoolean("is_active")).thenReturn(active);
        when(rs.getTimestamp("created_at")).thenReturn(null);
        when(rs.getString("photo_url")).thenReturn(null);
        when(rs.getString("title_ru")).thenReturn(null);
        when(rs.getString("title_kz")).thenReturn(null);
        when(rs.getString("description_ru")).thenReturn(null);
        when(rs.getString("description_kz")).thenReturn(null);
    }
}
