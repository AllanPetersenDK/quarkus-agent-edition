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
import dk.ashlan.agent.product.api.ProductApiException;
import dk.ashlan.agent.product.model.ProductAssistantQueryRequest;
import dk.ashlan.agent.product.model.ProductAssistantQueryResponse;
import dk.ashlan.agent.product.model.ProductConversationDetailResponse;
import dk.ashlan.agent.product.model.ProductConversationSummaryResponse;
import dk.ashlan.agent.product.model.ProductOperatorOverviewResponse;
import dk.ashlan.agent.product.service.ProductAssistantService;
import dk.ashlan.agent.product.store.JdbcProductConversationStore;
import dk.ashlan.agent.product.store.ProductConversationStore;
import dk.ashlan.agent.rag.Chunker;
import dk.ashlan.agent.rag.DocumentIngestionService;
import dk.ashlan.agent.rag.EmbeddingClient;
import dk.ashlan.agent.rag.InMemoryVectorStore;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.Retriever;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductAssistantPhase2Test {
    @TempDir
    Path tempDir;

    @Test
    void productQueryPersistsConversationStateAndExposesOperatorSummary() throws Exception {
        ProductPhase2Harness harness = harness();

        ProductAssistantQueryResponse first = harness.resource.query(
                new ProductAssistantQueryRequest("product-conversation", "Which text mentions PostgreSQL?", 2)
        );
        assertTrue(first.conversationCreated());
        assertEquals(1, first.conversationTurnCount());
        assertEquals("COMPLETED", first.status());
        assertTrue(first.failureReason() == null || first.failureReason().isBlank());
        assertEquals("answered_with_sources", first.outcomeCategory());
        assertTrue(first.approved());
        assertFalse(first.traceHighlights().isEmpty());

        ProductAssistantQueryResponse second = harness.resource.query(
                new ProductAssistantQueryRequest("product-conversation", "Which text mentions PostgreSQL?", 2)
        );
        assertFalse(second.conversationCreated());
        assertEquals(2, second.conversationTurnCount());
        assertEquals("product-conversation", second.conversationId());
        assertEquals(second.runId(), harness.store.load("product-conversation").orElseThrow().lastRunId());
        assertEquals(4, second.toolCount());
        assertFalse(second.traceHighlights().isEmpty());

        var state = harness.store.load("product-conversation").orElseThrow();
        assertEquals(2, state.turnCount());
        assertEquals(second.runId(), state.lastRunId());
        assertTrue(state.lastQualitySignals().stream().anyMatch(signal -> signal.contains("conversation:stored")));

        var summaries = harness.operator.listConversations(10);
        assertEquals(1, summaries.size());
        ProductConversationSummaryResponse summary = summaries.get(0);
        assertEquals("product-conversation", summary.conversationId());
        assertEquals(2, summary.turnCount());
        assertEquals(second.runId(), summary.lastRunId());

        ProductConversationDetailResponse detail = harness.operator.conversation("product-conversation");
        assertEquals(2, detail.turnCount());
        assertEquals(2, detail.turns().size());
        assertNotNull(detail.lastAnswer());
        assertTrue(detail.qualitySignals().stream().anyMatch(signal -> signal.startsWith("memory-write:")));

        ProductOperatorOverviewResponse overview = harness.operator.overview(5);
        assertEquals(1L, overview.conversationCount());
        assertEquals(1, overview.recentConversationCount());
        assertEquals("product-conversation", overview.latestConversationId());
        assertEquals(second.runId(), overview.latestRunId());
        assertEquals("COMPLETED", overview.latestStatus());
        assertTrue(overview.latestFailureReason() == null || overview.latestFailureReason().isBlank());
        assertEquals(1, overview.recentConversations().size());
        assertTrue(overview.signals().stream().anyMatch(signal -> signal.startsWith("conversationCount:")));
    }

    @Test
    void productRequestIsConstrainedAndFailureResponsesAreStructured() throws Exception {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertFalse(validator.validate(new ProductAssistantQueryRequest("product-conversation", " ", 3)).isEmpty());
        assertFalse(validator.validate(new ProductAssistantQueryRequest("product-conversation", "query", 11)).isEmpty());
        assertFalse(validator.validate(new ProductAssistantQueryRequest("bad space", "query", 3)).isEmpty());
        assertFalse(validator.validate(new ProductAssistantQueryRequest("product-conversation", "x".repeat(4101), 3)).isEmpty());

        ProductPhase2Harness harness = harness();
        ProductApiException invalidConversation = assertThrows(ProductApiException.class, () ->
                harness.resource.query(new ProductAssistantQueryRequest("bad space", "Which text mentions PostgreSQL?", 2))
        );
        assertEquals(400, invalidConversation.status());
        assertEquals("product_conversation_invalid", invalidConversation.errorCode());

        ProductApiException longQuery = assertThrows(ProductApiException.class, () ->
                harness.resource.query(new ProductAssistantQueryRequest("product-conversation", "x".repeat(4101), 2))
        );
        assertEquals(400, longQuery.status());
        assertEquals("product_query_too_long", longQuery.errorCode());
    }

    @Test
    void productPipelineFailuresSurfaceStructuredErrorMetadata() throws Exception {
        ProductPhase2Harness harness = harness();
        ProductAssistantService failingService = new ProductAssistantService(
                harness.ragService,
                harness.memoryService,
                harness.sessionManager,
                new PlannerService(),
                new ReflectionService(),
                null,
                new ProductConversationStore() {
                    @Override
                    public java.util.Optional<dk.ashlan.agent.product.model.ProductConversationState> load(String conversationId) {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public dk.ashlan.agent.product.model.ProductConversationState save(dk.ashlan.agent.product.model.ProductConversationState state) {
                        throw new IllegalStateException("product persistence unavailable");
                    }

                    @Override
                    public java.util.List<dk.ashlan.agent.product.model.ProductConversationState> list(int limit) {
                        return java.util.List.of();
                    }

                    @Override
                    public long count() {
                        return 0;
                    }
                }
        );

        ProductApiException exception = assertThrows(ProductApiException.class, () ->
                failingService.query(new ProductAssistantQueryRequest("product-conversation", "Which text mentions PostgreSQL?", 2))
        );

        assertEquals(503, exception.status());
        assertEquals("product_pipeline_failed", exception.errorCode());
        assertEquals("product-conversation", exception.conversationId());
        assertNotNull(exception.requestId());

        ProductApiErrorResponse response = (ProductApiErrorResponse) new ProductApiExceptionMapper().toResponse(exception).getEntity();
        assertEquals(503, response.status());
        assertEquals("product_pipeline_failed", response.errorCode());
        assertEquals("product-conversation", response.conversationId());
    }

    private ProductPhase2Harness harness() throws Exception {
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
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:file:" + tempDir.resolve("product-state").toAbsolutePath() + ";AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        ProductConversationStore store = new JdbcProductConversationStore(dataSource);
        ProductAssistantService service = new ProductAssistantService(
                ragService,
                memoryService,
                sessionManager,
                new PlannerService(),
                new ReflectionService(),
                null,
                store
        );
        return new ProductPhase2Harness(new ProductAssistantResource(service), new ProductOperatorResource(store), store, ragService, memoryService, sessionManager);
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

    private record ProductPhase2Harness(
            ProductAssistantResource resource,
            ProductOperatorResource operator,
            ProductConversationStore store,
            RagService ragService,
            MemoryService memoryService,
            SessionManager sessionManager
    ) {
    }
}
