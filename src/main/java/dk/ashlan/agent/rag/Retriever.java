package dk.ashlan.agent.rag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class Retriever {
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final HybridRetrievalScorer scorer;

    @Inject
    public Retriever(EmbeddingClient embeddingClient, VectorStore vectorStore, HybridRetrievalScorer scorer) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.scorer = scorer;
    }

    public Retriever(EmbeddingClient embeddingClient, VectorStore vectorStore) {
        this(embeddingClient, vectorStore, new HybridRetrievalScorer());
    }

    public List<RetrievalResult> retrieve(String query, int topK) {
        if (topK <= 0) {
            return List.of();
        }
        List<RetrievalResult> candidates = vectorStore.search(embeddingClient.embed(query), Integer.MAX_VALUE);
        return scorer.rerank(query, candidates, topK);
    }
}
