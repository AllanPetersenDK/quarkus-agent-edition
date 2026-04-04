package dk.ashlan.agent.eval;

import dk.ashlan.agent.code.CodeAgentOrchestrator;
import dk.ashlan.agent.code.CodeGenerationTool;
import dk.ashlan.agent.code.CodeWorkspaceRegistry;
import dk.ashlan.agent.code.CommandResult;
import dk.ashlan.agent.code.FileReadTool;
import dk.ashlan.agent.code.FileWriteTool;
import dk.ashlan.agent.code.GeneratedWorkspaceTool;
import dk.ashlan.agent.code.TestExecutionTool;
import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.multiagent.AgentRouter;
import dk.ashlan.agent.multiagent.Chapter9RunHistoryStore;
import dk.ashlan.agent.multiagent.CodingAgent;
import dk.ashlan.agent.multiagent.CoordinatorAgent;
import dk.ashlan.agent.multiagent.ResearchAgent;
import dk.ashlan.agent.multiagent.ReviewerAgent;
import dk.ashlan.agent.product.service.ProductAssistantService;
import dk.ashlan.agent.product.store.JdbcProductConversationStore;
import dk.ashlan.agent.product.store.ProductConversationStore;
import dk.ashlan.agent.planning.PlannerService;
import dk.ashlan.agent.planning.ReflectionService;
import dk.ashlan.agent.rag.Chunker;
import dk.ashlan.agent.rag.DocumentIngestionService;
import dk.ashlan.agent.rag.EmbeddingClient;
import dk.ashlan.agent.rag.InMemoryVectorStore;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.Retriever;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import dk.ashlan.agent.document.DocumentReadService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter10EvaluationServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void runExecutesCasesAcrossSupportedLanesAndRecordsAnEvaluationHistoryEntry() throws Exception {
        RuntimeRunHistoryStore historyStore = new RuntimeRunHistoryStore();
        RuntimeRunRecorder recorder = new RuntimeRunRecorder(historyStore);
        Chapter10EvaluationService service = harness(recorder, historyStore);

        Chapter10EvalRunResult result = service.run(new Chapter10EvalRunRequest(
                "chapter10-smoke",
                List.of(
                        new Chapter10EvalCase(
                                "manual-pass",
                                "manual",
                                "What is 25 * 4?",
                                "100",
                                List.of("stopReason:FINAL_ANSWER"),
                                0.5,
                                null
                        ),
                        new Chapter10EvalCase(
                                "product-pass",
                                "product",
                                "Which text mentions PostgreSQL?",
                                "PostgreSQL",
                                List.of("rag:retrieved:", "reflection:accepted"),
                                0.5,
                                2
                        ),
                        new Chapter10EvalCase(
                                "code-pass",
                                "code",
                                "write a hello-world style response",
                                "Workspace:",
                                List.of("validation:passed", "generatedTools:"),
                                0.5,
                                null
                        ),
                        new Chapter10EvalCase(
                                "multi-fail",
                                "multi-agent",
                                "x",
                                "Research summary",
                                List.of("review:approved"),
                                0.8,
                                null
                        )
                )
        ));

        assertEquals(4, result.total());
        assertEquals(3, result.passed());
        assertEquals(1, result.failed());
        assertTrue(result.qualitySignals().stream().anyMatch(signal -> signal.contains("cases:4")));
        assertTrue(result.qualitySignals().stream().anyMatch(signal -> signal.contains("failed:1")));
        assertEquals(4, result.results().size());
        assertTrue(result.results().stream().anyMatch(caseResult -> "manual-pass".equals(caseResult.caseId()) && caseResult.passed()));
        assertTrue(result.results().stream().anyMatch(caseResult -> "product-pass".equals(caseResult.caseId()) && caseResult.passed()));
        assertTrue(result.results().stream().anyMatch(caseResult -> "code-pass".equals(caseResult.caseId()) && caseResult.passed()));
        assertTrue(result.results().stream().anyMatch(caseResult -> "multi-fail".equals(caseResult.caseId()) && !caseResult.passed()));
        assertTrue(result.results().stream()
                .filter(caseResult -> "multi-fail".equals(caseResult.caseId()))
                .findFirst()
                .map(caseResult -> caseResult.failureReason() != null && caseResult.failureReason().contains("expected inspection signals"))
                .orElse(false));

        var evaluationRuns = historyStore.list("evaluation", 10);
        assertEquals(1, evaluationRuns.size());
        assertEquals(result.runId(), evaluationRuns.get(0).runId());
        assertEquals("evaluation", evaluationRuns.get(0).lane());
        assertTrue(evaluationRuns.get(0).qualitySignals().stream().anyMatch(signal -> signal.contains("score:")));
        assertTrue(evaluationRuns.get(0).traceSummary().contains("chapter10-run-start:" + result.runId()));
        assertTrue(historyStore.list("manual", 10).size() >= 1);
        assertTrue(historyStore.list("product", 10).size() >= 1);
        assertTrue(historyStore.list("code", 10).size() >= 1);
        assertTrue(historyStore.list("multi-agent", 10).size() >= 1);
    }

    private Chapter10EvaluationService harness(RuntimeRunRecorder recorder, RuntimeRunHistoryStore historyStore) throws Exception {
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
        ragService.ingestPath("docs/postgresql.txt", "docs/postgresql");

        SessionManager sessionManager = new SessionManager();
        MemoryService memoryService = new MemoryService(sessionManager, new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:file:" + tempDir.resolve("product-state").toAbsolutePath() + ";AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        ProductConversationStore productConversationStore = new JdbcProductConversationStore(dataSource);
        ProductAssistantService productAssistantService = new ProductAssistantService(
                ragService,
                memoryService,
                sessionManager,
                new PlannerService(),
                new ReflectionService(),
                recorder,
                productConversationStore
        );
        CodeAgentOrchestrator codeAgentOrchestrator = new CodeAgentOrchestrator(
                new CodeWorkspaceRegistry(tempDir.resolve("chapter8-workspaces").toString()),
                new FileReadTool(workspaceService),
                new FileWriteTool(workspaceService),
                new CodeGenerationTool(),
                new TestExecutionTool(workspaceService),
                recorder
        );
        CoordinatorAgent coordinatorAgent = new CoordinatorAgent(
                new AgentRouter(List.of(new ResearchAgent(), new CodingAgent())),
                new ReviewerAgent(),
                new Chapter9RunHistoryStore(),
                recorder
        );
        AgentOrchestrator manualOrchestrator = new AgentOrchestrator(
                (messages, toolRegistry, context) -> new LlmCompletion("25 multiplied by 4 is 100.", List.of()),
                new dk.ashlan.agent.tools.ToolRegistry(List.of()),
                new dk.ashlan.agent.tools.ToolExecutor(new dk.ashlan.agent.tools.ToolRegistry(List.of())),
                null,
                1,
                ""
        );

        return new Chapter10EvaluationService(
                manualOrchestrator,
                productAssistantService,
                codeAgentOrchestrator,
                coordinatorAgent,
                recorder,
                historyStore
        );
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
