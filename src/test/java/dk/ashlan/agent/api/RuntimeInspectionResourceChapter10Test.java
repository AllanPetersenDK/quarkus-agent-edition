package dk.ashlan.agent.api;

import dk.ashlan.agent.code.CodeAgentRunResult;
import dk.ashlan.agent.code.CommandResult;
import dk.ashlan.agent.code.GeneratedWorkspaceTool;
import dk.ashlan.agent.code.CodeWorkspaceRegistry;
import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.eval.Chapter10EvalCaseResult;
import dk.ashlan.agent.eval.RuntimeRunHistoryStore;
import dk.ashlan.agent.eval.RuntimeRunRecord;
import dk.ashlan.agent.eval.RuntimeRunRecorder;
import dk.ashlan.agent.eval.gaia.GaiaAttachment;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentStatus;
import dk.ashlan.agent.eval.gaia.GaiaCaseResult;
import dk.ashlan.agent.eval.gaia.GaiaLevelSummary;
import dk.ashlan.agent.eval.gaia.GaiaRunResult;
import dk.ashlan.agent.eval.gaia.GaiaScoreResult;
import dk.ashlan.agent.health.AgentReadinessHealthCheck;
import dk.ashlan.agent.health.RuntimeLivenessHealthCheck;
import dk.ashlan.agent.memory.InMemorySessionTraceStore;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryAwareAgentOrchestrator;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.multiagent.AgentTaskResult;
import dk.ashlan.agent.product.model.ProductAssistantQueryResponse;
import dk.ashlan.agent.product.model.ProductArtifactSummaryResponse;
import dk.ashlan.agent.product.model.ProductPlanResponse;
import dk.ashlan.agent.product.model.ProductReflectionResponse;
import dk.ashlan.agent.product.model.ProductSourceResponse;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeInspectionResourceChapter10Test {
    @Test
    void runHistoryListsLooksUpAndFiltersChapterTenRunsAcrossLanes() {
        RuntimeHarness harness = harness();
        RuntimeRunRecorder recorder = harness.recorder();

        Instant started = Instant.parse("2026-04-04T12:00:00Z");
        RuntimeRunRecord manual = recorder.recordManualRun(
                "manual-session",
                "What is 25 * 4?",
                new AgentRunResult(
                        "25 multiplied by 4 is 100.",
                        StopReason.FINAL_ANSWER,
                        1,
                        List.of("manual-trace", "tool:calculator", "answer: 100")
                ),
                started,
                started.plusMillis(5)
        );
        RuntimeRunRecord product = recorder.recordProductRun(
                "product-run-1",
                "product-conversation",
                "Which text mentions PostgreSQL?",
                        new ProductAssistantQueryResponse(
                                "product-run-1",
                                "product-conversation",
                                false,
                                2,
                        1,
                        "Which text mentions PostgreSQL?",
                        "PostgreSQL is an open-source relational database.",
                        List.of(new ProductSourceResponse("docs/postgresql", "docs/postgresql.txt", 0, "chunk-1", "PostgreSQL is an open-source relational database.", 0.91)),
                        List.of("memory:hints:1", "rag:retrieved:1"),
                                new ProductPlanResponse("Task plan", "Check sources", 2),
                                new ProductReflectionResponse(true, "Looks good."),
                                List.of("product-query-start", "rag:retrieved:1", "reflection:accepted"),
                                "COMPLETED",
                                null,
                                "Product query accepted with 1 sources, 1 memory hints, 2 plan steps. PostgreSQL is an open-source relational database.",
                                started.plusMillis(10),
                                started.plusMillis(15),
                                5L,
                                "conversation=product-conversation, created, retrievals=1, memoryHints=1, planSteps=2, reflection=accepted, note=accepted",
                                "retrievals=1, memoryHints=1, planSteps=2, reflection=accepted",
                                2,
                                List.of(
                                        new ProductArtifactSummaryResponse(
                                                "docs/postgresql:0:chunk-1",
                                                "product-run-1",
                                                "product-conversation",
                                                "knowledge-source",
                                                "docs/postgresql.txt",
                                                "text/plain",
                                                48L,
                                                started,
                                                "PostgreSQL is an open-source relational database.",
                                                "docs/postgresql.txt",
                                                "docs/postgresql"
                                        ),
                                        new ProductArtifactSummaryResponse(
                                                "product-run-1:summary",
                                                "product-run-1",
                                                "product-conversation",
                                                "assistant-summary",
                                                "Assistant answer",
                                                "text/plain",
                                                48L,
                                                started.plusMillis(10),
                                                "Product query accepted with 1 sources, 1 memory hints, 2 plan steps. PostgreSQL is an open-source relational database.",
                                                null,
                                                null
                                        )
                                )
                        ),
                started.plusMillis(10),
                started.plusMillis(15)
        );
        RuntimeRunRecord code = recorder.recordCodeRun(
                "code-run-1",
                "chapter8-demo",
                "write a hello-world style response",
                new CodeAgentRunResult(
                        "chapter8-demo",
                        "workspace-1",
                        "code-run-1",
                        "/tmp/chapter10/workspace",
                        "Workspace: /tmp/chapter10/workspace\nChapter 8 workspace response",
                        "generated/response.txt",
                        "generated/skills/workspace-summary.md",
                        "generated/tests/result.txt",
                        new CommandResult(0, "Validation passed: response, skill card, and generated tool registry are aligned.", ""),
                        3L,
                        List.of(new GeneratedWorkspaceTool(
                                "workspace-summary",
                                "Summarize the Chapter 8 workspace state.",
                                "Summarize the Chapter 8 workspace state.",
                                "generated/skills/workspace-summary.md",
                                List.of("generated/response.txt", "generated/skills/workspace-summary.md", "generated/tests/result.txt"),
                                started.plusMillis(11),
                                null,
                                0
                        )),
                        List.of("chapter10-run-start:code-run-1", "chapter10-lane:code", "validation:passed", "chapter10-run-complete:code-run-1")
                ),
                started.plusMillis(20),
                started.plusMillis(26)
        );
        RuntimeRunRecord multiAgent = recorder.recordMultiAgentRun(
                "chapter9-run-1",
                "x",
                new AgentTaskResult(
                        "chapter9-run-1",
                        started.plusMillis(30),
                        "x",
                        "research",
                        "Research summary for: x. Key angle: compare sources and capture stable facts.",
                        false,
                        "Reviewer: Rejected. Specialist output is too thin for the objective.",
                        "fallback to research because no clear specialist keyword was found",
                        "Coordinator created chapter9-run-1, routed to research, and reviewer rejected the specialist draft.",
                        List.of("chapter9-run-start:chapter9-run-1", "chapter9-route:research", "chapter9-review:rejected", "chapter9-run-complete:chapter9-run-1")
                ),
                started.plusMillis(30),
                started.plusMillis(34)
        );
        RuntimeRunRecord evaluation = recorder.recordChapter10EvaluationRun(
                "chapter10-eval-1",
                "chapter10 smoke evaluation",
                List.of(new Chapter10EvalCaseResult(
                        "product-case",
                        "product",
                        "product-run-1",
                        "Which text mentions PostgreSQL?",
                        "PostgreSQL is an open-source relational database.",
                        true,
                        1.0,
                        "Matched the expected output and inspection signals for lane product.",
                        null,
                        List.of("rag:retrieved:1", "reflection:accepted"),
                        List.of("chapter10-run-start:product-run-1", "chapter10-lane:product"),
                        "chapter10-run-start:product-run-1 | chapter10-lane:product"
                )),
                started.plusMillis(40),
                started.plusMillis(43)
        );
        RuntimeRunRecord gaia = recorder.recordGaiaRun(
                "gaia-run-1",
                new GaiaRunResult(
                        "gaia-run-1",
                        null,
                        "/tmp/gaia",
                        "2023",
                        "validation",
                        1,
                        10,
                        1,
                        1,
                        0,
                        12L,
                        new GaiaLevelSummary(1, 1, 0, 1.0, 1.0),
                        Map.of("1", new GaiaLevelSummary(1, 1, 0, 1.0, 1.0)),
                        List.of(),
                        List.of(new GaiaCaseResult(
                                "gaia-run-1",
                                "task-1",
                                "What is PostgreSQL?",
                                "1",
                                List.of("PostgreSQL"),
                                "PostgreSQL",
                                new GaiaScoreResult(true, 1.0, "match", "postgresql", "postgresql", "postgresql"),
                                true,
                                1,
                                "FINAL_ANSWER",
                                List.of("gaia:passed:true"),
                                List.of("attachment:present"),
                                new GaiaAttachment("attachment.txt", "/snapshot/attachment.txt", "/resolved/attachment.txt", GaiaAttachmentStatus.PRESENT, "present", List.of("attachment:present")),
                                Map.of("source", "gaia")
                        ))
                ),
                started.plusMillis(50),
                started.plusMillis(62)
        );

        var allRuns = harness.resource().runs(null, 10);
        assertEquals(6, allRuns.size());
        assertEquals(gaia.runId(), allRuns.get(0).runId());
        assertEquals("gaia", allRuns.get(0).lane());
        assertEquals(evaluation.runId(), allRuns.get(1).runId());
        assertEquals("evaluation", allRuns.get(1).lane());
        assertEquals(manual.runId(), allRuns.get(5).runId());

        var productRuns = harness.resource().runs("product", 10);
        assertEquals(1, productRuns.size());
        assertEquals(product.runId(), productRuns.get(0).runId());
        assertTrue(productRuns.get(0).qualitySignals().stream().anyMatch(signal -> signal.contains("reflection:accepted")));

        var manualRun = harness.resource().run(manual.runId());
        assertEquals("manual", manualRun.lane());
        assertEquals("COMPLETED", manualRun.status());
        assertTrue(manualRun.selectedTraceEntries().stream().anyMatch(entry -> entry.contains("manual-trace")));

        var rejectedRun = harness.resource().run(multiAgent.runId());
        assertEquals("multi-agent", rejectedRun.lane());
        assertFalse(rejectedRun.approved());
        assertEquals("review_rejected", rejectedRun.errorCategory());
        assertNotNull(rejectedRun.rejectionReason());
        assertTrue(rejectedRun.traceSummary().contains("chapter10-run-start:" + multiAgent.runId()));

        var evaluationRuns = harness.resource().runs("evaluation", 10);
        assertEquals(1, evaluationRuns.size());
        assertTrue(evaluationRuns.get(0).traceSummary().contains("chapter10-run-start:chapter10-eval-1"));

        var gaiaRuns = harness.resource().runs("gaia", 10);
        assertEquals(1, gaiaRuns.size());
        assertTrue(gaiaRuns.get(0).qualitySignals().stream().anyMatch(signal -> signal.contains("passed:1")));
    }

    private RuntimeHarness harness() {
        RuntimeRunHistoryStore historyStore = new RuntimeRunHistoryStore();
        RuntimeRunRecorder recorder = new RuntimeRunRecorder(historyStore);
        SessionManager sessionManager = new SessionManager();
        MemoryService memoryService = new MemoryService(sessionManager, new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        RuntimeInspectionResource resource = new RuntimeInspectionResource(
                new AgentReadinessHealthCheck(new AgentOrchestrator(null, null, null, null, 1, "") {
                }, new ToolRegistry(List.of())),
                new RuntimeLivenessHealthCheck(),
                sessionManager,
                memoryService,
                new InMemorySessionTraceStore(),
                new MemoryAwareAgentOrchestrator(
                        new AgentOrchestrator(null, null, null, null, 1, "") {
                        },
                        memoryService
                ),
                new CodeWorkspaceRegistry("target/test-chapter10-workspaces"),
                historyStore
        );
        return new RuntimeHarness(resource, recorder);
    }

    private record RuntimeHarness(RuntimeInspectionResource resource, RuntimeRunRecorder recorder) {
    }
}
