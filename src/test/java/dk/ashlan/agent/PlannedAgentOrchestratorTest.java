package dk.ashlan.agent;

import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.AgentRunner;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.planning.PlannedAgentOrchestrator;
import dk.ashlan.agent.planning.PlannerService;
import dk.ashlan.agent.planning.ReflectionService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannedAgentOrchestratorTest {
    @Test
    void planningGuideIsSentIntoTheFirstRunAndReflectionCanTriggerImprovementLoop() {
        AtomicReference<String> firstPrompt = new AtomicReference<>();
        AtomicReference<String> secondPrompt = new AtomicReference<>();
        AgentRunner runner = message -> {
            if (firstPrompt.get() == null) {
                firstPrompt.set(message);
                return new AgentRunResult("short", StopReason.FINAL_ANSWER, 1, java.util.List.of(message));
            }
            secondPrompt.set(message);
            return new AgentRunResult("This is a fuller answer with enough detail.", StopReason.FINAL_ANSWER, 2, java.util.List.of(message));
        };
        PlannedAgentOrchestrator orchestrator = new PlannedAgentOrchestrator(new PlannerService(), new ReflectionService(), runner);

        AgentRunResult result = orchestrator.run("Explain Quarkus agents");

        assertEquals(StopReason.FINAL_ANSWER, result.stopReason());
        assertTrue(firstPrompt.get().contains("Task plan"));
        assertTrue(firstPrompt.get().contains("Explain Quarkus agents"));
        assertTrue(secondPrompt.get().contains("Reflection feedback:"));
        assertTrue(result.finalAnswer().contains("fuller answer"));
    }

    @Test
    void failureRecoveryUsesTheReplannedPromptWhenReflectionRejectsTheFirstAnswer() {
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<String> secondPrompt = new AtomicReference<>();
        AgentRunner runner = message -> {
            if (attempts.getAndIncrement() == 0) {
                return new AgentRunResult("short", StopReason.FINAL_ANSWER, 1, java.util.List.of(message));
            }
            secondPrompt.set(message);
            return new AgentRunResult("This is a fuller answer after recovery.", StopReason.FINAL_ANSWER, 2, java.util.List.of(message));
        };
        PlannedAgentOrchestrator orchestrator = new PlannedAgentOrchestrator(new PlannerService(), new ReflectionService(), runner);

        AgentRunResult result = orchestrator.run("Recover from a failure in a multi-step research task");

        assertEquals(StopReason.FINAL_ANSWER, result.stopReason());
        assertTrue(secondPrompt.get().contains("Revised plan"));
        assertTrue(secondPrompt.get().contains("Reflection feedback"));
        assertTrue(result.finalAnswer().contains("recovery"));
    }
}
