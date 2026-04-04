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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class JdbcTaskMemoryStore implements TaskMemoryStore {
    private static final TypeReference<double[]> VECTOR_TYPE = new TypeReference<>() {
    };

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Inject
    public JdbcTaskMemoryStore(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        ensureSchema();
    }

    public JdbcTaskMemoryStore(DataSource dataSource) {
        this(dataSource, new ObjectMapper());
    }

    @Override
    public void save(TaskMemory taskMemory) {
        if (taskMemory == null) {
            return;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     merge into task_memory (
                         dedup_key,
                         session_id,
                         task,
                         memory,
                         task_summary,
                         approach,
                         final_answer,
                         correct,
                         error_analysis,
                         embedding_json
                     ) key(dedup_key)
                     values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, taskMemory.structuredDedupKey());
            statement.setString(2, taskMemory.sessionId());
            statement.setString(3, taskMemory.task());
            statement.setString(4, taskMemory.memory());
            statement.setString(5, taskMemory.taskSummary());
            statement.setString(6, taskMemory.approach());
            statement.setString(7, taskMemory.finalAnswer());
            if (taskMemory.correct() == null) {
                statement.setNull(8, java.sql.Types.BOOLEAN);
            } else {
                statement.setBoolean(8, taskMemory.correct());
            }
            statement.setString(9, taskMemory.errorAnalysis());
            statement.setString(10, objectMapper.writeValueAsString(TaskMemoryRanking.vectorize(taskMemory.searchableText())));
            statement.executeUpdate();
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to persist task memory for sessionId=" + taskMemory.sessionId(), exception);
        }
    }

    @Override
    public List<TaskMemory> findRelevant(String sessionId, String query, int limit) {
        String normalizedQuery = TaskMemoryRanking.normalize(query);
        if (normalizedQuery.isBlank() || limit <= 0) {
            return List.of();
        }

        double[] queryVector = TaskMemoryRanking.vectorize(query);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select session_id, task, memory, task_summary, approach, final_answer, correct, error_analysis, embedding_json
                     from task_memory
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            List<ScoredTaskMemory> scored = new ArrayList<>();
            while (resultSet.next()) {
                TaskMemory memory = new TaskMemory(
                        resultSet.getString("session_id"),
                        resultSet.getString("task"),
                        resultSet.getString("memory"),
                        resultSet.getString("task_summary"),
                        resultSet.getString("approach"),
                        resultSet.getString("final_answer"),
                        resultSet.getObject("correct") == null ? null : resultSet.getBoolean("correct"),
                        resultSet.getString("error_analysis")
                );
                scored.add(new ScoredTaskMemory(memory, score(queryVector, memory, resultSet.getString("embedding_json"), normalizedQuery)));
            }
            return scored.stream()
                    .sorted(Comparator.comparingDouble(ScoredTaskMemory::score).reversed())
                    .limit(limit)
                    .map(ScoredTaskMemory::memory)
                    .toList();
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to search task memories", exception);
        }
    }

    private double score(double[] queryVector, TaskMemory memory, String embeddingJson, String normalizedQuery) throws IOException {
        double[] storedVector = embeddingJson == null || embeddingJson.isBlank()
                ? new double[0]
                : objectMapper.readValue(embeddingJson, VECTOR_TYPE);
        double vectorScore = TaskMemoryRanking.cosineSimilarity(queryVector, storedVector) * 12.0d;
        return TaskMemoryRanking.score(memory, normalizedQuery) + vectorScore;
    }

    private void ensureSchema() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     create table if not exists task_memory (
                         dedup_key varchar(512) primary key,
                         session_id varchar(128) not null,
                         task clob not null,
                         memory clob not null,
                         task_summary clob,
                         approach clob,
                         final_answer clob,
                         correct boolean,
                         error_analysis clob,
                         embedding_json clob not null
                     )
                     """)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize task_memory schema", exception);
        }
    }

    private record ScoredTaskMemory(TaskMemory memory, double score) {
    }
}
