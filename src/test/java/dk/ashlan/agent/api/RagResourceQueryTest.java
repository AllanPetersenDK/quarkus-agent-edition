package dk.ashlan.agent.api;

import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.document.DocumentReadService;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import dk.ashlan.agent.rag.Chunker;
import dk.ashlan.agent.rag.DocumentIngestionService;
import dk.ashlan.agent.rag.EmbeddingClient;
import dk.ashlan.agent.rag.InMemoryVectorStore;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.Retriever;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagResourceQueryTest {
    @TempDir
    Path tempDir;

    @Test
    void queryResponseIncludesWinningChunkAndCitations() throws Exception {
        RagResource resource = new RagResource(ragService());
        Files.createDirectories(tempDir.resolve("workspace/docs"));
        Files.writeString(tempDir.resolve("workspace/docs/postgresql.txt"), """
                PostgreSQL is an open-source relational database.
                Quarkus is a Java framework optimized for cloud-native applications.
                """);
        Files.writeString(tempDir.resolve("workspace/docs/h2.txt"), """
                H2 is an embedded database often used for local development.
                """);

        resource.ingestPath(new RagResource.RagIngestPathRequest("docs/postgresql.txt", null));
        resource.ingestPath(new RagResource.RagIngestPathRequest("docs/h2.txt", null));

        RagResource.RagQueryResponse response = resource.query("Which text mentions PostgreSQL?", 2);

        assertEquals("Which text mentions PostgreSQL?", response.query());
        assertTrue(response.answer().contains("Source docs/postgresql.txt mentions PostgreSQL"));
        assertNotNull(response.bestChunk());
        assertEquals("docs/postgresql.txt", response.bestChunk().sourceId());
        assertFalse(response.citations().isEmpty());
        assertEquals("docs/postgresql.txt", response.citations().get(0).sourceId());
        assertEquals("docs/postgresql.txt", response.citations().get(0).sourcePath());
        assertFalse(response.citations().get(0).chunkId().isBlank());
        assertFalse(response.citations().get(0).sectionHint().isBlank());
    }

    private RagService ragService() throws Exception {
        Path workspaceRoot = tempDir.resolve("workspace");
        WorkspaceService workspaceService = new WorkspaceService(workspaceRoot.toString());
        DocumentReadService documentReadService = new DocumentReadService(workspaceService, new GaiaAttachmentExtractionService(), audioTranscriptionService());
        EmbeddingClient embeddingClient = new ConstantEmbeddingClient();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        DocumentIngestionService ingestionService = new DocumentIngestionService(new Chunker(), embeddingClient, vectorStore);
        Retriever retriever = new Retriever(embeddingClient, vectorStore);
        return new RagService(ingestionService, retriever, documentReadService);
    }

    private GaiaAudioTranscriptionService audioTranscriptionService() {
        return path -> "unused audio transcript";
    }

    private static final class ConstantEmbeddingClient implements EmbeddingClient {
        @Override
        public double[] embed(String text) {
            return new double[]{1.0, 1.0, 1.0};
        }
    }
}
