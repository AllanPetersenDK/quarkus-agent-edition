package dk.ashlan.agent.rag;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class Retriever {
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    public Retriever(EmbeddingClient embeddingClient, VectorStore vectorStore) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    public List<RetrievalResult> retrieve(String query, int topK) {
        return vectorStore.search(embeddingClient.embed(query), topK);
    }
}
