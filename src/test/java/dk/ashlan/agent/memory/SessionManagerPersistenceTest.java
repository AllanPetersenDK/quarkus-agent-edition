package dk.ashlan.agent.memory;

import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionManagerPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsSessionStateAcrossRestartLikeReload() throws Exception {
        JdbcDataSource dataSource = dataSource();
        createSchema(dataSource);
        JdbcSessionStateStore store = new JdbcSessionStateStore(dataSource);

        MemoryService memoryService = new MemoryService(new SessionManager(store), new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        memoryService.remember("session-1", "intro", "My name is Ada");

        SessionManager restarted = new SessionManager(store);
        assertTrue(restarted.session("session-1").messages().contains("My name is Ada"));
    }

    @Test
    void returnsEmptyStateForMissingSession() throws Exception {
        JdbcDataSource dataSource = dataSource();
        createSchema(dataSource);
        SessionManager sessionManager = new SessionManager(new JdbcSessionStateStore(dataSource));

        assertTrue(sessionManager.session("missing").messages().isEmpty());
    }

    @Test
    void surfacesCorruptStoredPayloads() throws Exception {
        JdbcDataSource dataSource = dataSource();
        createSchema(dataSource);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    merge into session_state (session_id, messages_json)
                    key(session_id)
                    values ('broken', 'not-json')
                    """);
        }

        JdbcSessionStateStore store = new JdbcSessionStateStore(dataSource);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> store.loadMessages("broken"));
        assertTrue(exception.getMessage().contains("Unable to load session state"));
    }

    private JdbcDataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:file:" + tempDir.resolve("session-state").toAbsolutePath() + ";AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        return dataSource;
    }

    private void createSchema(JdbcDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    create table if not exists session_state (
                        session_id varchar(128) primary key,
                        messages_json clob not null
                    )
                    """);
        }
    }
}
