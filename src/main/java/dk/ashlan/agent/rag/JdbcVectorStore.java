package dk.ashlan.agent.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Map;

@ApplicationScoped
public class JdbcVectorStore implements VectorStore {
    private static final TypeReference<Map<String, String>> METADATA_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<double[]> VECTOR_TYPE = new TypeReference<>() {
    };

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Inject
    public JdbcVectorStore(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    public JdbcVectorStore(DataSource dataSource) {
        this(dataSource, new ObjectMapper());
    }

    @Override
    public void add(DocumentChunk chunk, double[] vector) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     merge into rag_document_chunk (
                         chunk_id,
                         source_id,
                         chunk_index,
                         chunk_text,
                         metadata_json,
                         embedding_json
                     ) key(chunk_id)
                     values (?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, chunk.id());
            statement.setString(2, chunk.sourceId());
            statement.setInt(3, chunk.chunkIndex());
            statement.setString(4, chunk.text());
            statement.setString(5, serializeMetadata(chunk.metadata()));
            statement.setString(6, serializeVector(vector));
            statement.executeUpdate();
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to persist RAG chunk " + chunk.id(), exception);
        }
    }

    @Override
    public List<RetrievalResult> search(double[] vector, int topK) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select chunk_id, source_id, chunk_index, chunk_text, metadata_json, embedding_json
                     from rag_document_chunk
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            List<RetrievalResult> results = new ArrayList<>();
            while (resultSet.next()) {
                DocumentChunk chunk = new DocumentChunk(
                        resultSet.getString("chunk_id"),
                        resultSet.getString("source_id"),
                        resultSet.getInt("chunk_index"),
                        resultSet.getString("chunk_text"),
                        deserializeMetadata(resultSet.getString("metadata_json"))
                );
                double[] storedVector = deserializeVector(resultSet.getString("embedding_json"));
                if (storedVector.length != vector.length) {
                    throw new IOException("Embedding dimension mismatch for chunk " + chunk.id()
                            + ": expected " + vector.length + ", found " + storedVector.length);
                }
                results.add(new RetrievalResult(chunk, cosineSimilarity(vector, storedVector)));
            }
            return results.stream()
                    .sorted(Comparator.comparingDouble(RetrievalResult::similarity).reversed())
                    .limit(topK)
                    .toList();
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to search persisted RAG chunks", exception);
        }
    }

    private String serializeMetadata(Map<String, String> metadata) throws JsonProcessingException {
        return objectMapper.writeValueAsString(metadata);
    }

    private Map<String, String> deserializeMetadata(String json) throws IOException {
        if (json == null || json.isBlank()) {
            throw new IOException("Missing persisted RAG metadata payload");
        }
        return objectMapper.readValue(json, METADATA_TYPE);
    }

    private String serializeVector(double[] vector) throws JsonProcessingException {
        return objectMapper.writeValueAsString(vector);
    }

    private double[] deserializeVector(String json) throws IOException {
        if (json == null || json.isBlank()) {
            throw new IOException("Missing persisted RAG embedding payload");
        }
        return objectMapper.readValue(json, VECTOR_TYPE);
    }

    private double cosineSimilarity(double[] left, double[] right) {
        double dot = 0.0;
        double leftMagnitude = 0.0;
        double rightMagnitude = 0.0;
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            dot += left[i] * right[i];
            leftMagnitude += left[i] * left[i];
            rightMagnitude += right[i] * right[i];
        }
        if (leftMagnitude == 0 || rightMagnitude == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
    }
}
