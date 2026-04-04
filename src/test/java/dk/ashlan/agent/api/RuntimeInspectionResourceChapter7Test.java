package dk.ashlan.agent.api;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.health.AgentReadinessHealthCheck;
import dk.ashlan.agent.health.RuntimeLivenessHealthCheck;
import dk.ashlan.agent.llm.DemoToolCallingLlmClient;
import dk.ashlan.agent.memory.InMemorySessionTraceStore;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryAwareAgentOrchestrator;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.code.CodeWorkspaceRegistry;
import dk.ashlan.agent.planning.Chapter7ReflectionState;
import dk.ashlan.agent.planning.CreateTasksTool;
import dk.ashlan.agent.planning.ReflectionTool;
import dk.ashlan.agent.planning.PlannerService;
import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeInspectionResourceChapter7Test {
    @Test
    void planAndReflectionEndpointsExposeTheCurrentChapterSevenRuntimeState() {
        RuntimeHarness harness = harness();

        harness.sessionManager.session("chapter7-plan").setChapter7Plan(new PlannerService().plan("Create a task plan and reflect on it for a multi-step chapter 7 answer about Quarkus agents."));

        RuntimeInspectionResource.SessionPlanInspectionResponse plan = harness.resource.plan("chapter7-plan");
        assertEquals("chapter7-plan", plan.sessionId());
        assertEquals("active", plan.status());
        assertTrue(plan.goal().contains("Quarkus agents"));
        assertEquals(3, plan.steps().size());
        assertEquals(2, plan.nextActiveStep().order());
        assertTrue(plan.steps().get(1).status().contains("in_progress"));
        assertTrue(plan.steps().get(1).doneWhen().contains("facts or sources"));

        harness.sessionManager.session("chapter7-recovery").setChapter7Reflection(new Chapter7ReflectionState(
                "error_analysis",
                "The tool failed before we could finish the work.",
                false,
                true,
                false,
                "Revise the failed step and retry with a valid expression.",
                "Rebuild the plan around the corrected input.",
                "Reflection recorded (ERROR ANALYSIS) (REPLAN NEEDED): The tool failed before we could finish the work."
        ));

        RuntimeInspectionResource.SessionReflectionInspectionResponse reflection = harness.resource.reflection("chapter7-recovery");
        assertEquals("chapter7-recovery", reflection.sessionId());
        assertEquals("available", reflection.status());
        assertEquals("error_analysis", reflection.mode());
        assertTrue(reflection.needReplan());
        assertFalse(reflection.readyToAnswer());
        assertTrue(reflection.summary().contains("REPLAN NEEDED"));
        assertNotNull(reflection.alternativeDirection());
    }

    @Test
    void missingChapterSevenStateIsReportedExplicitly() {
        RuntimeHarness harness = harness();

        RuntimeInspectionResource.SessionPlanInspectionResponse plan = harness.resource.plan("unknown-session");
        assertEquals("unknown-session", plan.sessionId());
        assertEquals("missing", plan.status());
        assertTrue(plan.goal().isBlank());
        assertTrue(plan.steps().isEmpty());
        assertTrue(plan.nextActiveStep() == null);

        RuntimeInspectionResource.SessionReflectionInspectionResponse reflection = harness.resource.reflection("unknown-session");
        assertEquals("unknown-session", reflection.sessionId());
        assertEquals("missing", reflection.status());
        assertTrue(reflection.mode().isBlank());
        assertTrue(reflection.analysis().isBlank());
        assertFalse(reflection.accepted());
        assertFalse(reflection.needReplan());
        assertFalse(reflection.readyToAnswer());
        assertTrue(reflection.summary().isBlank());
    }

    private RuntimeHarness harness() {
        SessionManager sessionManager = new SessionManager();
        MemoryService memoryService = new MemoryService(sessionManager, new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new CalculatorTool(), new CreateTasksTool(), new ReflectionTool()));
        AgentOrchestrator agentOrchestrator = new AgentOrchestrator(
                new DemoToolCallingLlmClient(),
                toolRegistry,
                new ToolExecutor(toolRegistry),
                memoryService,
                sessionManager,
                5,
                "chapter 7 system prompt"
        );
        RuntimeInspectionResource resource = new RuntimeInspectionResource(
                new AgentReadinessHealthCheck(agentOrchestrator, toolRegistry),
                new RuntimeLivenessHealthCheck(),
                sessionManager,
                memoryService,
                new InMemorySessionTraceStore(),
                new MemoryAwareAgentOrchestrator(agentOrchestrator, memoryService),
                new CodeWorkspaceRegistry("target/test-chapter8-workspaces")
        );
        return new RuntimeHarness(resource, new MemoryAwareAgentOrchestrator(agentOrchestrator, memoryService), sessionManager);
    }

    private record RuntimeHarness(
            RuntimeInspectionResource resource,
            MemoryAwareAgentOrchestrator orchestrator,
            SessionManager sessionManager
    ) {
    }
}
