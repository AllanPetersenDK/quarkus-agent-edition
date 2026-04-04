package dk.ashlan.agent.agents;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.AgentRunner;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.memory.MemoryAwareAgentOrchestrator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentsFacadeTest {
    @Test
    void chapterFourCallbackAgentTracksLifecycle() {
        AgentRunner runner = message -> new AgentRunResult("answer", StopReason.FINAL_ANSWER, 1, java.util.List.of(message));
        AgentOrchestrator orchestrator = new AgentOrchestratorAdapter(runner);
        ToolCallingAgentCh4Callback agent = new ToolCallingAgentCh4Callback(orchestrator);

        AgentRunResult result = agent.run("hello");

        assertEquals(StopReason.FINAL_ANSWER, result.stopReason());
        assertTrue(agent.callbacks().get(0).startsWith("before:"));
    }

    @Test
    void chapterSixFacadeDelegatesToMemoryAwareAgent() {
        MemoryAwareAgentOrchestrator orchestrator = new MemoryAwareAgentOrchestrator(
                new AgentOrchestratorAdapter(message -> new AgentRunResult("memory answer", StopReason.FINAL_ANSWER, 1, java.util.List.of(message))),
                new dk.ashlan.agent.memory.MemoryService(
                        new dk.ashlan.agent.memory.SessionManager(),
                        new dk.ashlan.agent.memory.InMemoryTaskMemoryStore(),
                        new dk.ashlan.agent.memory.MemoryExtractionService()
                )
        );
        ToolCallingAgentCh6 agent = new ToolCallingAgentCh6(orchestrator);

        AgentRunResult result = agent.run("session-1", "hello");

        assertEquals("memory answer", result.finalAnswer());
    }

    @Test
    void chapterFourConvenienceRunStillUsesDefaultCompatibilitySession() {
        AgentOrchestrator orchestrator = new AgentOrchestrator(null, null, null, null, 1, "") {
            @Override
            public AgentRunResult run(String message, String sessionId) {
                return new AgentRunResult(message + ":" + sessionId, StopReason.FINAL_ANSWER, 1, java.util.List.of(message));
            }
        };

        AgentRunResult result = orchestrator.run("hello");

        assertEquals("hello:default", result.finalAnswer());
    }

    private static final class AgentOrchestratorAdapter extends AgentOrchestrator {
        private final AgentRunner runner;

        private AgentOrchestratorAdapter(AgentRunner runner) {
            super(null, null, null, null, 1, "");
            this.runner = runner;
        }

        @Override
        public AgentRunResult run(String message) {
            return runner.run(message);
        }

        @Override
        public AgentRunResult run(String message, String sessionId) {
            return runner.run(message + ":" + sessionId);
        }
    }
}
