package dk.ashlan.agent.product.api;

import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.document.DocumentReadService;
import dk.ashlan.agent.eval.RuntimeRunHistoryStore;
import dk.ashlan.agent.eval.RuntimeRunRecorder;
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
import dk.ashlan.agent.product.model.ProductArtifactCollectionResponse;
import dk.ashlan.agent.product.model.ProductConversationDetailResponse;
import dk.ashlan.agent.product.model.ProductConversationSummaryResponse;
import dk.ashlan.agent.product.model.ProductOverviewResponse;
import dk.ashlan.agent.product.model.ProductRunDetailResponse;
import dk.ashlan.agent.product.service.ProductAssistantService;
import dk.ashlan.agent.product.service.ProductLaneService;
import dk.ashlan.agent.product.store.JdbcProductConversationStore;
import dk.ashlan.agent.product.store.ProductConversationStore;
import dk.ashlan.agent.rag.Chunker;
import dk.ashlan.agent.rag.DocumentIngestionService;
import dk.ashlan.agent.rag.EmbeddingClient;
import dk.ashlan.agent.rag.InMemoryVectorStore;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.Retriever;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductLanePhase2Test {
    @TempDir
    Path tempDir;

    @Test
    void canonicalProductLaneListsDetailsRunsAndArtifacts() throws Exception {
        ProductHarness harness = harness();

        ProductAssistantQueryResponse first = harness.assistantResource.query(
                new ProductAssistantQueryRequest("product-conversation", "Which text mentions PostgreSQL?", 2)
        );
        ProductAssistantQueryResponse second = harness.assistantResource.query(
                new ProductAssistantQueryRequest("product-conversation", "Which text mentions PostgreSQL?", 2)
        );

        List<ProductConversationSummaryResponse> conversations = harness.laneResource.conversations(10);
        assertEquals(1, conversations.size());
        ProductConversationSummaryResponse summary = conversations.get(0);
        assertEquals("product-conversation", summary.conversationId());
        assertEquals(2, summary.turnCount());
        assertEquals(second.runId(), summary.lastRunId());
        assertNotNull(summary.summary());
        assertNotNull(summary.traceSummary());
        assertTrue(summary.artifactCount() > 0);

        ProductConversationDetailResponse detail = harness.laneResource.conversation("product-conversation");
        assertEquals(2, detail.turnCount());
        assertEquals(2, detail.turns().size());
        assertTrue(detail.artifactCount() > 0);
        assertFalse(detail.artifacts().isEmpty());
        assertNotNull(detail.summary());
        assertNotNull(detail.traceSummary());

        ProductRunDetailResponse runDetail = harness.laneResource.run(first.runId());
        assertEquals(first.runId(), runDetail.runId());
        assertEquals("product-conversation", runDetail.conversationId());
        assertFalse(runDetail.artifacts().isEmpty());
        assertFalse(runDetail.sources().isEmpty());
        assertNotNull(runDetail.summary());
        assertNotNull(runDetail.traceSummary());

        ProductArtifactCollectionResponse artifactCollection = harness.laneResource.runArtifacts(first.runId());
        assertEquals(runDetail.artifacts().size(), artifactCollection.artifactCount());
        assertFalse(artifactCollection.artifacts().isEmpty());

        ProductOverviewResponse overview = harness.laneResource.overview(5);
        assertEquals(1L, overview.totalConversations());
        assertTrue(overview.totalRuns() >= 2);
        assertEquals(1, overview.recentConversations().size());
        assertFalse(overview.recentRuns().isEmpty());
        assertFalse(overview.recentArtifacts().isEmpty());
        assertEquals("UP", overview.health().status());
        assertTrue(overview.signals().stream().anyMatch(signal -> signal.startsWith("conversationCount:")));
        assertTrue(overview.signals().stream().anyMatch(signal -> signal.startsWith("runCount:")));
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
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:file:" + tempDir.resolve("product-state").toAbsolutePath() + ";AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        ProductConversationStore store = new JdbcProductConversationStore(dataSource);
        RuntimeRunHistoryStore runHistoryStore = new RuntimeRunHistoryStore();
        RuntimeRunRecorder recorder = new RuntimeRunRecorder(runHistoryStore);
        ProductAssistantService service = new ProductAssistantService(
                ragService,
                memoryService,
                sessionManager,
                new PlannerService(),
                new ReflectionService(),
                recorder,
                store
        );
        return new ProductHarness(new ProductAssistantResource(service), new ProductLaneResource(new ProductLaneService(store, runHistoryStore)));
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
            ProductAssistantResource assistantResource,
            ProductLaneResource laneResource
    ) {
    }
}
