package dk.ashlan.agent.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {
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
    public Optional<List<String>> loadMessages(String sessionId) {
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
                    return Optional.of(List.of());
                }
                return Optional.of(objectMapper.readValue(json, LIST_OF_STRINGS));
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
            statement.setString(2, objectMapper.writeValueAsString(sessionState.messages()));
            statement.executeUpdate();
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to persist session state for " + sessionState.sessionId(), exception);
        }
    }
}
