package dk.ashlan.agent.rag;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class RagService {
    private final DocumentIngestionService ingestionService;
    private final Retriever retriever;

    public RagService(DocumentIngestionService ingestionService, Retriever retriever) {
        this.ingestionService = ingestionService;
        this.retriever = retriever;
    }

    public List<DocumentChunk> ingest(String sourceId, String text) {
        return ingestionService.ingest(sourceId, text);
    }

    public List<RetrievalResult> retrieve(String query, int topK) {
        return retriever.retrieve(query, topK);
    }

    public String answer(String query, int topK) {
        List<RetrievalResult> results = retrieve(query, topK);
        if (results.isEmpty()) {
            return "No relevant knowledge found.";
        }
        return results.stream()
                .map(result -> result.chunk().text())
                .collect(Collectors.joining("\n\n"));
    }
}
