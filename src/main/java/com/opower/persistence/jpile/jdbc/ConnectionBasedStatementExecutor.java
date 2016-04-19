package com.opower.persistence.jpile.jdbc;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Implementation of {@link StatementExecutor} interface that operates the
 * JDBC {@link Connection}. Foreign keys are disabled for the duration of
 * usage of {@link ConnectionBasedStatementExecutor} instance.
 * <p/>
 * Don't forget to {@link #shutdown()} the instance to ensure that foreign key
 * constraints are re-enabled.
 * <p/>
 * <b>Important</b>: connection is not closed in {@link #shutdown()} because it is not
 * created within {@link ConnectionBasedStatementExecutor} and that's caller's responsibility to
 * manage connection lifecycle.
 *
 * @author ivan.german
 */
public class ConnectionBasedStatementExecutor implements StatementExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionBasedStatementExecutor.class);

    private final Connection connection;

    public ConnectionBasedStatementExecutor(Connection connection) {
        Preconditions.checkNotNull(connection, "connection can't be null");

        this.connection = connection;
        init();
    }

    protected Connection getConnection() {
        return this.connection;
    }

    protected void init() {
        execute(ToggleForeignKeysStatementCallback.DISABLE_FOREIGN_KEYS);
    }

    /**
     * Force close the connection. Connection is not closed in {@link #shutdown()} because it is not
     * created within {@link ConnectionBasedStatementExecutor} and it is caller's responsibility to
     * manage connection lifecycle.
     *
     * This method is redundant for {@link StatementExecutor} interface but can be used for other
     * {@link StatementExecutor} that uses this exact implementation (e.g. {@link DataSourceBasedStatementExecutor}).
     */
    public void closeConnection() {
        try {
            this.connection.close();
        }
        catch (SQLException e) {
            // Ignored exception
            LOG.warn("Exception while force-closing connection", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T execute(StatementCallback<T> statementCallback) {
        Preconditions.checkNotNull(statementCallback, "can't execute null statementCallback");

        Connection actualConnection = getConnection();

        try (Statement statement = actualConnection.createStatement()) {
            return statementCallback.doInStatement(statement);
        }
        catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Re-enables foreign key constraints for current connection.
     * <p/>
     * <b>Important</b>: connection is not closed here because it is not
     * created within {@link ConnectionBasedStatementExecutor} and it is
     * caller's responsibility to manage connection lifecycle.
     */
    @Override
    public void shutdown() {
        execute(ToggleForeignKeysStatementCallback.ENABLE_FOREIGN_KEYS);
    }
}
