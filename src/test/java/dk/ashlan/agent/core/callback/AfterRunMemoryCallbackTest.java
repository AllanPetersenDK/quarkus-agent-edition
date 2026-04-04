package dk.ashlan.agent.core.callback;

import dk.ashlan.agent.core.AfterRunContext;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.memory.TaskMemory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AfterRunMemoryCallbackTest {
    @Test
    void persistsRunSignalAfterCompletion() {
        MemoryService memoryService = new MemoryService(
                new SessionManager(),
                new InMemoryTaskMemoryStore(),
                new MemoryExtractionService()
        );
        AfterRunMemoryCallback callback = new AfterRunMemoryCallback(memoryService);
        AgentRunResult result = new AgentRunResult(
                "25 * 4 = 100",
                StopReason.FINAL_ANSWER,
                2,
                List.of(
                        "iteration:1",
                        "tool:calculator:25 * 4",
                        "answer: 25 * 4 = 100"
                )
        );

        callback.afterRun(new AfterRunContext(
                "session-1",
                "What is 25 * 4?",
                result,
                result.trace()
        ));

        TaskMemory stored = memoryService.longTermMemories("session-1", "25 * 4", 1).get(0);
        assertTrue(stored.approach().contains("after-run"));
        assertTrue(stored.problem().contains("What is 25 * 4?"));
        assertTrue(stored.summary().contains("What is 25 * 4?"));
        assertTrue(stored.result().contains("100"));
        assertTrue(memoryService.relevantMemories("session-1", "25 * 4").stream()
                .anyMatch(memory -> memory.contains("25 * 4")));
        assertTrue(memoryService.relevantMemories("session-1", "25 * 4").stream()
                .noneMatch(memory -> memory.contains("trace:")));
    }

    @Test
    void ephemeralSessionsDoNotPersistRunSignals() {
        MemoryService memoryService = new MemoryService(
                new SessionManager(),
                new InMemoryTaskMemoryStore(),
                new MemoryExtractionService()
        );
        AfterRunMemoryCallback callback = new AfterRunMemoryCallback(memoryService);
        AgentRunResult result = new AgentRunResult(
                "25 * 4 = 100",
                StopReason.FINAL_ANSWER,
                2,
                List.of("iteration:1", "answer: 25 * 4 = 100")
        );

        callback.afterRun(new AfterRunContext(
                "ephemeral-123",
                "What is 25 * 4?",
                result,
                result.trace()
        ));

        assertTrue(memoryService.longTermMemories("ephemeral-123", "25 * 4", 1).isEmpty());
        assertTrue(memoryService.relevantMemories("ephemeral-123", "25 * 4").isEmpty());
    }
}
