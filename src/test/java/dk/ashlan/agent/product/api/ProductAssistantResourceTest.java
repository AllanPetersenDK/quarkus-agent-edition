package dk.ashlan.agent.product.api;

import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.document.DocumentReadService;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.planning.PlannerService;
import dk.ashlan.agent.planning.ReflectionService;
import dk.ashlan.agent.product.model.ProductAssistantQueryRequest;
import dk.ashlan.agent.product.model.ProductAssistantQueryResponse;
import dk.ashlan.agent.product.service.ProductAssistantService;
import dk.ashlan.agent.rag.Chunker;
import dk.ashlan.agent.rag.DocumentIngestionService;
import dk.ashlan.agent.rag.EmbeddingClient;
import dk.ashlan.agent.rag.InMemoryVectorStore;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.Retriever;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductAssistantResourceTest {
    @TempDir
    Path tempDir;

    @Test
    void queryReturnsAStableProductContractAndUsesCoreCapabilities() throws Exception {
        ProductHarness harness = harness();

        harness.memoryService.remember("product-conversation", "goal", "Remember that my favorite database is PostgreSQL.");

        ProductAssistantQueryResponse response = harness.resource.query(
                new ProductAssistantQueryRequest("product-conversation", "Which text mentions PostgreSQL?", 2)
        );

        assertEquals("product-conversation", response.conversationId());
        assertEquals("Which text mentions PostgreSQL?", response.query());
        assertTrue(response.answer().contains("PostgreSQL"));
        assertEquals(2, response.conversationMessageCount());
        assertFalse(response.sources().isEmpty());
        assertEquals("docs/postgresql", response.sources().get(0).sourceId());
        assertEquals("docs/postgresql.txt", response.sources().get(0).sourcePath());
        assertFalse(response.sources().get(0).excerpt().isBlank());
        assertFalse(response.memoryHints().isEmpty());
        assertTrue(response.memoryHints().get(0).contains("PostgreSQL"));
        assertTrue(response.plan().summary().contains("Task plan"));
        assertFalse(response.plan().nextStep().isBlank());
        assertEquals(3, response.plan().stepCount());
        assertTrue(response.reflection().accepted());
        assertFalse(response.traceHighlights().isEmpty());
        assertEquals("answered_with_sources", response.outcomeCategory());
        assertEquals(2, response.sourceCount());
        assertEquals(2, response.citationCount());
        assertEquals(2, response.retrievalCount());
        assertEquals(4, response.toolCount());
        assertEquals(3, response.planStepCount());
        assertTrue(response.approved());
        assertTrue(response.signals().contains("product-query-start"));
        assertTrue(response.signals().contains("conversation:product-conversation"));
        assertTrue(response.signals().stream().anyMatch(signal -> signal.startsWith("rag:retrieved:")));
        assertTrue(response.signals().stream().anyMatch(signal -> signal.startsWith("memory-write:")));
        assertEquals(2, harness.sessionManager.session("product-conversation").size());
        assertFalse(harness.memoryService.longTermMemories("product-conversation", "PostgreSQL", 3).isEmpty());
    }

    @Test
    void productRequestRequiresARealQuery() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertFalse(validator.validate(new ProductAssistantQueryRequest("product-conversation", " ", 3)).isEmpty());
    }

    private ProductHarness harness() throws Exception {
        Path workspaceRoot = tempDir.resolve("workspace");
        WorkspaceService workspaceService = new WorkspaceService(workspaceRoot.toString());
        DocumentReadService documentReadService = new DocumentReadService(workspaceService, new GaiaAttachmentExtractionService(), audioTranscriptionService());
        EmbeddingClient embeddingClient = new ConstantEmbeddingClient();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        DocumentIngestionService ingestionService = new DocumentIngestionService(new Chunker(), embeddingClient, vectorStore);
        Retriever retriever = new Retriever(embeddingClient, vectorStore);
        RagService ragService = new RagService(ingestionService, retriever, documentReadService);

        Files.createDirectories(workspaceRoot.resolve("docs"));
        Files.writeString(workspaceRoot.resolve("docs/postgresql.txt"), """
                PostgreSQL is an open-source relational database.
                Quarkus is a Java framework optimized for cloud-native applications.
                """);
        Files.writeString(workspaceRoot.resolve("docs/h2.txt"), """
                H2 is an embedded database often used for local development.
                """);

        ragService.ingestPath("docs/postgresql.txt", "docs/postgresql");
        ragService.ingestPath("docs/h2.txt", "docs/h2");

        SessionManager sessionManager = new SessionManager();
        MemoryService memoryService = new MemoryService(sessionManager, new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        ProductAssistantService service = new ProductAssistantService(
                ragService,
                memoryService,
                sessionManager,
                new PlannerService(),
                new ReflectionService()
        );
        return new ProductHarness(new ProductAssistantResource(service), memoryService, sessionManager);
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

    private record ProductHarness(
            ProductAssistantResource resource,
            MemoryService memoryService,
            SessionManager sessionManager
    ) {
    }
}
