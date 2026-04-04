package dk.ashlan.agent.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.ashlan.agent.llm.LlmMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcSessionStateStore implements SessionStateStore {
    private static final TypeReference<SessionStateSnapshot> SNAPSHOT = new TypeReference<>() {
    };
    private static final TypeReference<List<LlmMessage>> LIST_OF_MESSAGES = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> LIST_OF_LEGACY_MESSAGES = new TypeReference<>() {
    };

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Inject
    public JdbcSessionStateStore(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    public JdbcSessionStateStore(DataSource dataSource) {
        this(dataSource, new ObjectMapper());
    }

    @Override
    public Optional<SessionStateSnapshot> load(String sessionId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select messages_json
                     from session_state
                     where session_id = ?
                     """)) {
            statement.setString(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                String json = resultSet.getString(1);
                if (json == null || json.isBlank()) {
                    return Optional.of(new SessionStateSnapshot(List.of(), List.of()));
                }
                return Optional.of(readSnapshot(json));
            }
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to load session state for " + sessionId, exception);
        }
    }

    @Override
    public void save(SessionState sessionState) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     merge into session_state (session_id, messages_json)
                     key(session_id)
                     values (?, ?)
                     """)) {
            statement.setString(1, sessionState.sessionId());
            statement.setString(2, objectMapper.writeValueAsString(new SessionStateSnapshot(sessionState.messages(), sessionState.pendingToolCalls())));
            statement.executeUpdate();
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to persist session state for " + sessionState.sessionId(), exception);
        }
    }

    private SessionStateSnapshot readSnapshot(String json) throws IOException {
        try {
            return objectMapper.readValue(json, SNAPSHOT);
        } catch (IOException exception) {
            // fall through to legacy parsing
        }
        try {
            List<LlmMessage> messages = objectMapper.readValue(json, LIST_OF_MESSAGES);
            return new SessionStateSnapshot(messages, List.of());
        } catch (IOException exception) {
            try {
                List<String> legacyMessages = objectMapper.readValue(json, LIST_OF_LEGACY_MESSAGES);
                List<LlmMessage> messages = legacyMessages.stream().map(LlmMessage::user).toList();
                return new SessionStateSnapshot(messages, List.of());
            } catch (IOException legacyException) {
                throw exception;
            }
        }
    }
}
