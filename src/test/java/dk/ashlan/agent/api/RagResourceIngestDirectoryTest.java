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

class RagResourceIngestDirectoryTest {
    @TempDir
    Path tempDir;

    @Test
    void ingestDirectoryResourceReturnsStructuredSummary() throws Exception {
        RagResource resource = new RagResource(ragService());
        writeFile("docs/a.txt", "Alpha document mentions quarkus.");
        writeFile("docs/skip.bin", "noise");

        RagResource.RagDirectoryIngestRequest request = new RagResource.RagDirectoryIngestRequest("docs", "samples", false, 20);
        dk.ashlan.agent.rag.RagDirectoryIngestResult response = resource.ingestDirectory(request);

        assertEquals("docs", response.path());
        assertTrue(response.totalCandidates() >= 2);
        assertTrue(response.results().stream().anyMatch(item -> "INGESTED".equals(item.status())));
        assertTrue(response.results().stream().anyMatch(item -> "SKIPPED_UNSUPPORTED".equals(item.status())));
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

    private void writeFile(String relativePath, String contents) throws Exception {
        Path file = tempDir.resolve("workspace").resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents);
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
