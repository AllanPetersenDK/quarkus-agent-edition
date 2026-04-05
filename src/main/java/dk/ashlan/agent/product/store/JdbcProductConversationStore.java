package dk.ashlan.agent.product.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.ashlan.agent.product.model.ProductConversationState;
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
public class JdbcProductConversationStore implements ProductConversationStore {
    private static final TypeReference<ProductConversationState> STATE_TYPE = new TypeReference<>() {
    };

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Inject
    public JdbcProductConversationStore(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        ensureSchema();
    }

    public JdbcProductConversationStore(DataSource dataSource) {
        this(dataSource, new ObjectMapper().findAndRegisterModules());
    }

    @Override
    public Optional<ProductConversationState> load(String conversationId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select state_json
                     from product_conversation_state
                     where conversation_id = ?
                     """)) {
            statement.setString(1, conversationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                String json = resultSet.getString(1);
                if (json == null || json.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(objectMapper.readValue(json, STATE_TYPE));
            }
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to load product conversation " + conversationId, exception);
        }
    }

    @Override
    public ProductConversationState save(ProductConversationState state) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     merge into product_conversation_state (
                         conversation_id,
                         created_at,
                         updated_at,
                         state_json
                     ) key(conversation_id)
                     values (?, ?, ?, ?)
                     """)) {
            statement.setString(1, state.conversationId());
            statement.setObject(2, state.createdAt());
            statement.setObject(3, state.updatedAt());
            statement.setString(4, serializeState(state));
            statement.executeUpdate();
            return state;
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to persist product conversation " + state.conversationId(), exception);
        }
    }

    @Override
    public List<ProductConversationState> list(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select state_json
                     from product_conversation_state
                     order by updated_at desc
                     fetch first ? rows only
                     """)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ProductConversationState> states = new ArrayList<>();
                while (resultSet.next()) {
                    String json = resultSet.getString(1);
                    if (json == null || json.isBlank()) {
                        continue;
                    }
                    states.add(objectMapper.readValue(json, STATE_TYPE));
                }
                return List.copyOf(states);
            }
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to list product conversations", exception);
        }
    }

    @Override
    public long count() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select count(*)
                     from product_conversation_state
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return 0L;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to count product conversations", exception);
        }
    }

    private String serializeState(ProductConversationState state) throws JsonProcessingException {
        return objectMapper.writeValueAsString(state);
    }

    private void ensureSchema() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     create table if not exists product_conversation_state (
                         conversation_id varchar(128) primary key,
                         created_at timestamp not null,
                         updated_at timestamp not null,
                         state_json clob not null
                     )
                     """)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize product_conversation_state schema", exception);
        }
    }
}
