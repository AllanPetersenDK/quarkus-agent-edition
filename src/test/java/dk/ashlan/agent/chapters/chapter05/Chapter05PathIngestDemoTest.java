package dk.ashlan.agent.chapters.chapter05;

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

class Chapter05PathIngestDemoTest {
    @TempDir
    Path tempDir;

    @Test
    void demoIngestsPathThenAnswersFromTheWorkspaceDocument() throws Exception {
        Path workspaceRoot = tempDir.resolve("workspace");
        WorkspaceService workspaceService = new WorkspaceService(workspaceRoot.toString());
        DocumentReadService documentReadService = new DocumentReadService(workspaceService, new GaiaAttachmentExtractionService(), audioTranscriptionService());
        EmbeddingClient embeddingClient = new ConstantEmbeddingClient();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        RagService ragService = new RagService(new DocumentIngestionService(new Chunker(), embeddingClient, vectorStore), new Retriever(embeddingClient, vectorStore), documentReadService);
        Chapter05PathIngestDemo demo = new Chapter05PathIngestDemo(ragService);

        Files.createDirectories(workspaceRoot.resolve("docs"));
        Files.writeString(workspaceRoot.resolve("docs/answer.txt"), "The secret keyword is quarkus.");

        Chapter05PathIngestDemo.Chapter05PathIngestResult result = demo.run("What is the secret keyword?", "docs/answer.txt", null);

        assertEquals("INGESTED", result.status());
        assertTrue(result.answer().contains("quarkus"));
        assertTrue(result.trace().contains("attachment:text-extracted"));
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
