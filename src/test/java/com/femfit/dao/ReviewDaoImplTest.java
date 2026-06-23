package com.femfit.dao;

import com.femfit.dao.impl.ReviewDaoImpl;
import com.femfit.datasource.ConnectionPool;
import com.femfit.model.Review;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ReviewDaoImpl}.
 *
 * <p>All JDBC dependencies are mocked with Mockito — no real database needed.
 * Tests verify correct SQL parameter binding, ResultSet-to-model mapping,
 * edge cases (empty result, null trainerId), and exception propagation.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewDaoImpl tests")
class ReviewDaoImplTest {

    @Mock private ConnectionPool pool;
    @Mock private Connection conn;
    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;

    @InjectMocks private ReviewDaoImpl dao;

    @BeforeEach
    void setUp() throws SQLException {
        when(pool.getConnection()).thenReturn(conn);
    }

    // ── save ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save: sets generated id and createdAt from RETURNING clause")
    void save_setsGeneratedFields() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong("id")).thenReturn(10L);
        Timestamp ts = Timestamp.valueOf("2026-06-01 12:00:00");
        when(rs.getTimestamp("created_at")).thenReturn(ts);

        Review review = Review.builder()
                .orderId(1L).memberId(2L).trainerId(3L)
                .rating(5).comment("Excellent!").build();

        Review saved = dao.save(review);

        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getCreatedAt()).isNotNull();
        verify(ps).setLong(1, 1L);
        verify(ps).setLong(2, 2L);
        verify(ps).setLong(3, 3L);
        verify(ps).setInt(4, 5);
        verify(ps).setString(5, "Excellent!");
        verify(pool).releaseConnection(conn);
    }

    @Test
    @DisplayName("save: uses setNull when trainerId is null")
    void save_nullTrainerId_usesSetNull() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong("id")).thenReturn(11L);
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf("2026-06-01 12:00:00"));

        Review review = Review.builder()
                .orderId(1L).memberId(2L).trainerId(null)
                .rating(4).comment("Good").build();

        dao.save(review);

        verify(ps).setNull(3, Types.BIGINT);
        verify(pool).releaseConnection(conn);
    }

    // ── findByOrderId ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findByOrderId: returns Optional with review when found")
    void findByOrderId_found_returnsReview() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        mockReviewRow(rs, 1L, 1L, 2L, 5);

        Optional<Review> result = dao.findByOrderId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getRating()).isEqualTo(5);
        verify(ps).setLong(1, 1L);
        verify(pool).releaseConnection(conn);
    }

    @Test
    @DisplayName("findByOrderId: returns empty Optional when no review exists")
    void findByOrderId_notFound_returnsEmpty() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        Optional<Review> result = dao.findByOrderId(99L);

        assertThat(result).isEmpty();
        verify(pool).releaseConnection(conn);
    }

    // ── findByMemberId ────────────────────────────────────────────────────

    @Test
    @DisplayName("findByMemberId: returns all reviews for a member")
    void findByMemberId_returnsList() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        mockReviewRow(rs, 1L, 1L, 5L, 4);

        List<Review> result = dao.findByMemberId(5L);

        assertThat(result).hasSize(2);
        verify(ps).setLong(1, 5L);
        verify(pool).releaseConnection(conn);
    }

    @Test
    @DisplayName("findByMemberId: returns empty list when member has no reviews")
    void findByMemberId_noReviews_returnsEmpty() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        List<Review> result = dao.findByMemberId(99L);

        assertThat(result).isEmpty();
        verify(pool).releaseConnection(conn);
    }

    // ── findRecentForDisplay ──────────────────────────────────────────────

    @Test
    @DisplayName("findRecentForDisplay: sets LIMIT parameter and returns list")
    void findRecentForDisplay_setsLimit() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        mockReviewRow(rs, 1L, 1L, 2L, 5);

        List<Review> result = dao.findRecentForDisplay(3);

        assertThat(result).hasSize(1);
        verify(ps).setInt(1, 3);
        verify(pool).releaseConnection(conn);
    }

    // ── existsByOrderId ───────────────────────────────────────────────────

    @Test
    @DisplayName("existsByOrderId: returns true when review exists")
    void existsByOrderId_exists_returnsTrue() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getBoolean(1)).thenReturn(true);

        assertThat(dao.existsByOrderId(1L)).isTrue();
        verify(ps).setLong(1, 1L);
        verify(pool).releaseConnection(conn);
    }

    @Test
    @DisplayName("existsByOrderId: returns false when no review for order")
    void existsByOrderId_notExists_returnsFalse() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getBoolean(1)).thenReturn(false);

        assertThat(dao.existsByOrderId(99L)).isFalse();
        verify(pool).releaseConnection(conn);
    }

    @Test
    @DisplayName("existsByOrderId: returns false when ResultSet is empty")
    void existsByOrderId_emptyResultSet_returnsFalse() throws SQLException {
        when(conn.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThat(dao.existsByOrderId(5L)).isFalse();
        verify(pool).releaseConnection(conn);
    }

    // ── exception handling ────────────────────────────────────────────────

    @Test
    @DisplayName("findByOrderId: wraps SQLException in RuntimeException")
    void findByOrderId_sqlException_throwsRuntimeException() throws SQLException {
        when(conn.prepareStatement(any())).thenThrow(new SQLException("connection lost"));

        assertThatThrownBy(() -> dao.findByOrderId(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed");

        verify(pool).releaseConnection(conn);
    }

    @Test
    @DisplayName("save: wraps SQLException in RuntimeException")
    void save_sqlException_throwsRuntimeException() throws SQLException {
        when(conn.prepareStatement(any())).thenThrow(new SQLException("unique constraint"));

        Review review = Review.builder()
                .orderId(1L).memberId(1L).rating(3).comment("ok").build();

        assertThatThrownBy(() -> dao.save(review))
                .isInstanceOf(RuntimeException.class);

        verify(pool).releaseConnection(conn);
    }

    @Test
    @DisplayName("findByMemberId: wraps SQLException in RuntimeException")
    void findByMemberId_sqlException_throwsRuntimeException() throws SQLException {
        when(conn.prepareStatement(any())).thenThrow(new SQLException("DB error"));

        assertThatThrownBy(() -> dao.findByMemberId(1L))
                .isInstanceOf(RuntimeException.class);

        verify(pool).releaseConnection(conn);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private void mockReviewRow(ResultSet rs, long id, long orderId, long memberId, int rating)
            throws SQLException {
        when(rs.getLong("id")).thenReturn(id);
        when(rs.getLong("order_id")).thenReturn(orderId);
        when(rs.getLong("member_id")).thenReturn(memberId);
        when(rs.getObject("trainer_id")).thenReturn(null);
        when(rs.getInt("rating")).thenReturn(rating);
        when(rs.getString("comment")).thenReturn("Great workout");
        when(rs.getTimestamp("created_at")).thenReturn(null);
        when(rs.getString("member_name")).thenReturn("Anna K");
        when(rs.getString("cycle_title")).thenReturn("Yoga Flow");
        when(rs.getString("cycle_title_ru")).thenReturn("Йога");
        when(rs.getString("cycle_title_kz")).thenReturn("Йога KZ");
    }
}
