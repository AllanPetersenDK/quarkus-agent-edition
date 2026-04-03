package dk.ashlan.agent.api;

import dk.ashlan.agent.rag.Chunker;
import dk.ashlan.agent.rag.DocumentIngestionService;
import dk.ashlan.agent.rag.FakeEmbeddingClient;
import dk.ashlan.agent.rag.InMemoryVectorStore;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.Retriever;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagResourceTest {
    @Test
    void ingestAndQueryExposeTheRagOuterFlow() {
        Chunker chunker = new Chunker();
        FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        RagService ragService = new RagService(
                new DocumentIngestionService(chunker, embeddingClient, vectorStore),
                new Retriever(embeddingClient, vectorStore)
        );
        RagResource resource = new RagResource(ragService);

        RagResource.RagIngestResponse ingest = resource.ingest(new RagResource.RagIngestRequest(
                "chapter-05",
                "Quarkus is a fast Java framework. The calculator tool evaluates arithmetic expressions."
        ));
        RagResource.RagQueryResponse query = resource.query("calculator", 1);

        assertEquals("chapter-05", ingest.sourceId());
        assertEquals(1, ingest.chunkCount());
        assertTrue(query.answer().contains("calculator"));
        assertEquals(1, query.chunks().size());
    }
}
