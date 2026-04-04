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
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagResourceIngestPathTest {
    @TempDir
    Path tempDir;

    @Test
    void ingestPathResourceReturnsStructuredMetadata() throws Exception {
        Path workspaceRoot = tempDir.resolve("workspace");
        WorkspaceService workspaceService = new WorkspaceService(workspaceRoot.toString());
        DocumentReadService documentReadService = new DocumentReadService(workspaceService, new GaiaAttachmentExtractionService(), audioTranscriptionService());
        EmbeddingClient embeddingClient = new ConstantEmbeddingClient();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        RagService ragService = new RagService(new DocumentIngestionService(new Chunker(), embeddingClient, vectorStore), new Retriever(embeddingClient, vectorStore), documentReadService);
        RagResource resource = new RagResource(ragService);

        Files.createDirectories(workspaceRoot.resolve("docs"));
        Files.writeString(workspaceRoot.resolve("docs/sample.txt"), "Hello from the workspace.");

        RagResource.RagIngestPathResponse response = resource.ingestPath(new RagResource.RagIngestPathRequest("docs/sample.txt", null));

        assertEquals("INGESTED", response.status());
        assertEquals("docs/sample.txt", response.path());
        assertEquals("docs/sample.txt", response.sourceId());
        assertTrue(response.chunkCount() > 0);
        assertTrue(response.chunks().get(0).text().contains("Hello from the workspace."));
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
