package dk.ashlan.agent.chapters.chapter07;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.llm.DemoToolCallingLlmClient;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.planning.CreateTasksTool;
import dk.ashlan.agent.planning.ReflectionTool;
import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter07RuntimeSeamTest {
    @Test
    void planningAndReflectionCycleRunsThroughTheExistingAgentLoop() {
        ToolRegistry toolRegistry = new ToolRegistry(List.of(
                new CalculatorTool(),
                new CreateTasksTool(),
                new ReflectionTool()
        ));
        MemoryService memoryService = new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                new DemoToolCallingLlmClient(),
                toolRegistry,
                new ToolExecutor(toolRegistry),
                memoryService,
                4,
                "chapter 7 system prompt"
        );

        AgentRunResult result = orchestrator.run("Create a task plan and reflect on it for a multi-step chapter 7 answer about Quarkus agents.", "chapter7-cycle");

        assertEquals(StopReason.FINAL_ANSWER, result.stopReason());
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("tool:create-tasks:")));
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("tool:reflection:")));
        assertTrue(result.finalAnswer().contains("Planning/reflection complete:"));
    }

    @Test
    void simpleQuestionsStillSkipChapterSevenPlanningTools() {
        ToolRegistry toolRegistry = new ToolRegistry(List.of(
                new CalculatorTool(),
                new CreateTasksTool(),
                new ReflectionTool()
        ));
        MemoryService memoryService = new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                new DemoToolCallingLlmClient(),
                toolRegistry,
                new ToolExecutor(toolRegistry),
                memoryService,
                3,
                "chapter 7 system prompt"
        );

        AgentRunResult result = orchestrator.run("What is 25 * 4?", "chapter7-simple");

        assertEquals(StopReason.FINAL_ANSWER, result.stopReason());
        assertTrue(result.finalAnswer().contains("25 * 4 = 100"));
        assertTrue(result.trace().stream().noneMatch(entry -> entry.contains("create-tasks")));
        assertTrue(result.trace().stream().noneMatch(entry -> entry.contains("reflection")));
    }
}
