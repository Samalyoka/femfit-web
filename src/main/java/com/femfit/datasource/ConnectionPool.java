package com.femfit.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe custom JDBC Connection Pool.
 *
 * <p>Uses a {@link BlockingQueue} to hold available connections.
 * Connections are pre-created at startup and returned to the pool
 * after use. ORM frameworks (JPA/Hibernate) are NOT used.</p>
 *
 * <p>Design pattern applied: <strong>Singleton</strong> (via Spring @Bean)
 * + <strong>Object Pool</strong> pattern.</p>
 *
 * @author FemFit Team
 * @version 1.0.0
 */
public class ConnectionPool {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);

    private final String url;
    private final String username;
    private final String password;
    private final int poolSize;

    /** Queue of available (idle) connections. */
    private final BlockingQueue<Connection> availableConnections;

    /** All connections created by this pool (for shutdown). */
    private final List<Connection> allConnections;

    private volatile boolean isShutdown = false;

    /**
     * Creates and initialises the connection pool.
     *
     * @param url      JDBC connection URL
     * @param username database username
     * @param password database password
     * @param driver   JDBC driver class name
     * @param poolSize number of connections to pre-create
     * @throws RuntimeException if driver cannot be loaded or connections fail
     */
    public ConnectionPool(String url, String username, String password,
                          String driver, int poolSize) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
        this.availableConnections = new ArrayBlockingQueue<>(poolSize);
        this.allConnections = new ArrayList<>(poolSize);

        loadDriver(driver);
        initConnections();
        log.info("ConnectionPool initialised with {} connections to {}", poolSize, url);
    }

    private void loadDriver(String driver) {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC driver not found: " + driver, e);
        }
    }

    private void initConnections() {
        for (int i = 0; i < poolSize; i++) {
            try {
                Connection conn = DriverManager.getConnection(url, username, password);
                availableConnections.add(conn);
                allConnections.add(conn);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create connection #" + i, e);
            }
        }
    }

    /**
     * Acquires a connection from the pool.
     * Blocks up to 30 seconds if no connection is available.
     *
     * @return a JDBC {@link Connection}
     * @throws RuntimeException if the pool is shut down or timeout expires
     */
    public Connection getConnection() {
        if (isShutdown) {
            throw new IllegalStateException("ConnectionPool is shut down");
        }
        try {
            Connection conn = availableConnections.poll(30, TimeUnit.SECONDS);
            if (conn == null) {
                throw new RuntimeException("Timeout: no connection available in pool");
            }
            // Recreate if connection was closed externally
            if (conn.isClosed()) {
                conn = DriverManager.getConnection(url, username, password);
                allConnections.add(conn);
            }
            log.debug("Connection acquired. Available: {}/{}", availableConnections.size(), poolSize);
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for connection", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to validate/recreate connection", e);
        }
    }

    /**
     * Returns a connection back to the pool.
     * Must be called in a finally block to prevent connection leaks.
     *
     * @param connection the connection to release
     */
    public void releaseConnection(Connection connection) {
        if (connection != null && !isShutdown) {
            try {
                // Ensure no dangling transactions
                if (!connection.getAutoCommit()) {
                    connection.rollback();
                    connection.setAutoCommit(true);
                }
                availableConnections.offer(connection);
                log.debug("Connection released. Available: {}/{}", availableConnections.size(), poolSize);
            } catch (SQLException e) {
                log.error("Error releasing connection: {}", e.getMessage());
            }
        }
    }

    /**
     * Returns number of connections currently available in the pool.
     *
     * @return available connection count
     */
    public int getAvailableCount() {
        return availableConnections.size();
    }

    /**
     * Shuts down the pool and closes all connections.
     * Called automatically by Spring when context is destroyed (@Bean destroyMethod).
     */
    public void shutdown() {
        isShutdown = true;
        for (Connection conn : allConnections) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.warn("Error closing connection during shutdown: {}", e.getMessage());
            }
        }
        log.info("ConnectionPool shut down. All {} connections closed.", allConnections.size());
    }
}