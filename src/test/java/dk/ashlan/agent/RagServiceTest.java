package dk.ashlan.agent;

import dk.ashlan.agent.rag.Chunker;
import dk.ashlan.agent.rag.DocumentIngestionService;
import dk.ashlan.agent.rag.DocumentChunk;
import dk.ashlan.agent.rag.FakeEmbeddingClient;
import dk.ashlan.agent.rag.InMemoryVectorStore;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.Retriever;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagServiceTest {
    @Test
    void retrievalReturnsRelevantChunk() {
        Chunker chunker = new Chunker();
        FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        DocumentIngestionService ingestionService = new DocumentIngestionService(chunker, embeddingClient, vectorStore);
        Retriever retriever = new Retriever(embeddingClient, vectorStore);
        RagService ragService = new RagService(ingestionService, retriever);

        List<DocumentChunk> chunks = ragService.ingest("docs", """
                Quarkus is a fast Java framework for cloud-native applications.

                The calculator tool evaluates arithmetic expressions.
                """);

        assertFalse(chunks.isEmpty());
        assertTrue(ragService.retrieve("arithmetic expressions", 1).get(0).chunk().text().contains("calculator"));
    }
}
