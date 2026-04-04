package dk.ashlan.agent.rag;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DocumentIngestionService {
    private final Chunker chunker;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    public DocumentIngestionService(Chunker chunker, EmbeddingClient embeddingClient, VectorStore vectorStore) {
        this.chunker = chunker;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    public List<DocumentChunk> ingest(String sourceId, String text) {
        return ingest(sourceId, text, Map.of());
    }

    public List<DocumentChunk> ingest(String sourceId, String text, Map<String, String> documentMetadata) {
        List<DocumentChunk> chunks = chunker.chunk(sourceId, text, 400, documentMetadata);
        chunks.forEach(chunk -> vectorStore.add(chunk, embeddingClient.embed(chunk.text())));
        return chunks;
    }
}
