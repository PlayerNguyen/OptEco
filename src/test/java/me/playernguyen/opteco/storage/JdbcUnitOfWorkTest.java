package me.playernguyen.opteco.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import me.playernguyen.opteco.storage.jdbc.JdbcUnitOfWork;
import me.playernguyen.opteco.storage.jdbc.SqliteConnectionProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdbcUnitOfWorkTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRollbackWhenTransactionWorkThrows() throws Exception {
        // Arrange
        SqliteConnectionProvider provider =
                new SqliteConnectionProvider(tempDir.resolve("unit-of-work.db").toFile());
        try (Connection connection = provider.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sample (value INTEGER NOT NULL)");
        }
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork(provider);

        // Act
        assertThrows(
                IllegalStateException.class,
                () -> unitOfWork.execute(connection -> {
                    insert(connection, 1);
                    throw new IllegalStateException("boom");
                }));

        // Assert
        assertEquals(0, count(provider));
        provider.close();
    }

    @Test
    void shouldCommitWhenTransactionWorkCompletes() throws Exception {
        // Arrange
        SqliteConnectionProvider provider =
                new SqliteConnectionProvider(tempDir.resolve("commit.db").toFile());
        try (Connection connection = provider.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sample (value INTEGER NOT NULL)");
        }
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork(provider);

        // Act
        unitOfWork.execute(connection -> {
            insert(connection, 7);
            return null;
        });

        // Assert
        assertEquals(1, count(provider));
        provider.close();
    }

    private void insert(Connection connection, int value) throws java.sql.SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO sample (value) VALUES (" + value + ")");
        }
    }

    private int count(SqliteConnectionProvider provider) throws Exception {
        try (Connection connection = provider.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM sample")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
