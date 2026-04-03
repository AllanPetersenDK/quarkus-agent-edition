package dk.ashlan.agent.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.ashlan.agent.core.AgentStepResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcSessionTraceStore implements SessionTraceStore {
    private static final TypeReference<List<AgentStepResult>> LIST_OF_STEPS = new TypeReference<>() {
    };

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Inject
    public JdbcSessionTraceStore(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    public JdbcSessionTraceStore(DataSource dataSource) {
        this(dataSource, new ObjectMapper());
    }

    @Override
    public Optional<List<AgentStepResult>> load(String sessionId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select steps_json
                     from session_step_trace
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
                return Optional.of(objectMapper.readValue(json, LIST_OF_STEPS));
            }
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to load session trace for " + sessionId, exception);
        }
    }

    @Override
    public void append(AgentStepResult stepResult) {
        List<AgentStepResult> current = load(stepResult.sessionId()).orElse(List.of());
        List<AgentStepResult> updated = new ArrayList<>(current);
        updated.add(stepResult);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     merge into session_step_trace (session_id, steps_json)
                     key(session_id)
                     values (?, ?)
                     """)) {
            statement.setString(1, stepResult.sessionId());
            statement.setString(2, objectMapper.writeValueAsString(updated));
            statement.executeUpdate();
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to persist session trace for " + stepResult.sessionId(), exception);
        }
    }
}
