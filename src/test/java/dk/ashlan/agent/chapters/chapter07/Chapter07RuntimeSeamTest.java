package dk.ashlan.agent.chapters.chapter07;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmToolCall;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter07RuntimeSeamTest {
    @Test
    void planningReflectionAndThinAnswerControlAreVisibleInTrace() {
        AgentRunResult result = orchestrator().run("Create a task plan and reflect on it for a multi-step chapter 7 answer about Quarkus agents.", "chapter7-cycle");

        assertEquals(StopReason.FINAL_ANSWER, result.stopReason());
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("chapter7-plan:")));
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("chapter7-reflection:")));
        assertTrue(result.finalAnswer().contains("Planning/reflection complete:"));
    }

    @Test
    void failureRecoveryTriggersAReplanBeforeTheFinalAnswer() {
        AgentRunResult result = orchestrator().run("Create a task plan and recover from a failure while solving 2 / 0 for chapter 7.", "chapter7-recovery");

        assertEquals(StopReason.FINAL_ANSWER, result.stopReason());
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("chapter7-plan:")));
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("chapter7-reflection:")));
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("chapter7-replan:")));
        assertTrue(result.finalAnswer().contains("Planning/reflection complete:"));
    }

    @Test
    void simpleQuestionsStillSkipChapterSevenPlanningTools() {
        AgentRunResult result = orchestrator().run("What is 25 * 4?", "chapter7-simple");

        assertEquals(StopReason.FINAL_ANSWER, result.stopReason());
        assertTrue(result.finalAnswer().contains("25 * 4 = 100"));
        assertTrue(result.trace().stream().noneMatch(entry -> entry.contains("chapter7-plan")));
        assertTrue(result.trace().stream().noneMatch(entry -> entry.contains("chapter7-reflection")));
    }

    @Test
    void chapterSevenRecoveryCanRecordAnExplicitReplanOnToolFailure() {
        AtomicInteger invocations = new AtomicInteger();
        LlmClient llmClient = (messages, toolRegistry, context) -> {
            int invocation = invocations.getAndIncrement();
            if (invocation == 0) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("create-tasks", chapterSevenRecoveryPlan(), "call-1")));
            }
            if (invocation == 1) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall(
                        "calculator",
                        Map.of("expression", "2 / 0"),
                        "call-2"
                )));
            }
            return LlmCompletion.answer("Recovered.");
        };

        AgentRunResult result = orchestrator(llmClient).run("Create a task plan and recover from a failure while solving 2 / 0 for chapter 7.", "chapter7-recovery");

        assertEquals(StopReason.FINAL_ANSWER, result.stopReason());
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("chapter7-plan:")));
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("chapter7-replan:")));
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("chapter7-reflection:")));
    }

    private Map<String, Object> chapterSevenRecoveryPlan() {
        java.util.List<Map<String, Object>> tasks = new java.util.ArrayList<>();

        Map<String, Object> firstTask = new java.util.LinkedHashMap<>();
        firstTask.put("content", "Attempt the division and observe the failure");
        firstTask.put("status", "in_progress");
        firstTask.put("doneWhen", "The failure is observed");
        firstTask.put("notes", "Start from the error");
        tasks.add(firstTask);

        Map<String, Object> secondTask = new java.util.LinkedHashMap<>();
        secondTask.put("content", "Recover with a safer strategy");
        secondTask.put("status", "pending");
        secondTask.put("doneWhen", "A recovery direction is identified");
        secondTask.put("notes", "Switch methods");
        tasks.add(secondTask);

        Map<String, Object> arguments = new java.util.LinkedHashMap<>();
        arguments.put("goal", "Solve 2 / 0 and recover for chapter 7.");
        arguments.put("tasks", tasks);
        return arguments;
    }

    private AgentOrchestrator orchestrator() {
        return orchestrator(new DemoToolCallingLlmClient());
    }

    private AgentOrchestrator orchestrator(LlmClient llmClient) {
        ToolRegistry toolRegistry = new ToolRegistry(List.of(
                new CalculatorTool(),
                new CreateTasksTool(),
                new ReflectionTool()
        ));
        MemoryService memoryService = new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        return new AgentOrchestrator(
                llmClient,
                toolRegistry,
                new ToolExecutor(toolRegistry),
                memoryService,
                5,
                "chapter 7 system prompt"
        );
    }
}
