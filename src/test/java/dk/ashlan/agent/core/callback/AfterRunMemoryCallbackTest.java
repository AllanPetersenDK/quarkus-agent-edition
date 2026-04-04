package dk.ashlan.agent.core.callback;

import dk.ashlan.agent.core.AfterRunContext;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
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
                "Your favorite database is PostgreSQL.",
                StopReason.FINAL_ANSWER,
                2,
                List.of(
                        "iteration:1",
                        "tool:web-search:[compacted tool output; originalChars=2048] result",
                        "answer: Your favorite database is PostgreSQL."
                )
        );

        callback.afterRun(new AfterRunContext(
                "session-1",
                "Remember that my favorite database is PostgreSQL.",
                result,
                result.trace()
        ));

        assertTrue(memoryService.relevantMemories("session-1", "PostgreSQL").stream()
                .anyMatch(memory -> memory.contains("trace:")));
    }
}
