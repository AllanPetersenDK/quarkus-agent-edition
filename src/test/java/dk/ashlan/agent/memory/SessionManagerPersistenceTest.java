package dk.ashlan.agent.memory;

import dk.ashlan.agent.core.PendingToolCall;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmToolCall;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionManagerPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsSessionStateAcrossRestartLikeReload() throws Exception {
        JdbcDataSource dataSource = dataSource();
        createSchema(dataSource);
        JdbcSessionStateStore store = new JdbcSessionStateStore(dataSource);

        SessionManager sessionManager = new SessionManager(store);
        sessionManager.session("session-1").addUserMessage("Hello");
        sessionManager.session("session-1").addAssistantMessage("Hi Ada");
        sessionManager.session("session-1").addPendingToolCall(new PendingToolCall(
                "session-1",
                1,
                "Hello",
                new LlmToolCall("confirmable", java.util.Map.of("value", "approved"), "call-1"),
                "Approve confirmable?"
        ));

        SessionManager restarted = new SessionManager(store);
        assertEquals(2, restarted.session("session-1").messages().size());
        assertEquals("Hello", restarted.session("session-1").messages().get(0).content());
        assertEquals("Hi Ada", restarted.session("session-1").messages().get(1).content());
        assertEquals(1, restarted.session("session-1").pendingToolCalls().size());
        assertEquals("call-1", restarted.session("session-1").pendingToolCalls().get(0).toolCall().callId());
    }

    @Test
    void clearedPendingToolCallsArePersistedAsCleared() throws Exception {
        JdbcDataSource dataSource = dataSource();
        createSchema(dataSource);
        JdbcSessionStateStore store = new JdbcSessionStateStore(dataSource);

        SessionManager sessionManager = new SessionManager(store);
        SessionState session = sessionManager.session("session-clear");
        session.addPendingToolCall(new PendingToolCall(
                "session-clear",
                1,
                "Hello",
                new LlmToolCall("confirmable", java.util.Map.of("value", "approved"), "call-1"),
                "Approve confirmable?"
        ));
        assertEquals(1, session.pendingToolCalls().size());
        session.clearPendingToolCalls();

        SessionManager restarted = new SessionManager(store);
        assertTrue(restarted.session("session-clear").pendingToolCalls().isEmpty());
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

    @Test
    void surfacesDatastoreProblems() {
        JdbcSessionStateStore store = new JdbcSessionStateStore(new FailingDataSource());
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> store.loadMessages("session-1"));
        assertTrue(exception.getMessage().contains("Unable to load session state"));
    }

    @Test
    void handlesConcurrentSessionUpdatesWithoutLosingMessages() throws Exception {
        SessionManager sessionManager = new SessionManager();
        var session = sessionManager.session("concurrent");
        var executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(8);
        List<LlmMessage> expected = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            String message = "message-" + i;
            expected.add(LlmMessage.user(message));
            executor.submit(() -> {
                session.addUserMessage(message);
                latch.countDown();
            });
        }

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertEquals(8, session.size());
        assertTrue(session.messages().containsAll(expected));
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

    private static final class FailingDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("boom");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLException("boom");
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getAnonymousLogger();
        }
    }
}
