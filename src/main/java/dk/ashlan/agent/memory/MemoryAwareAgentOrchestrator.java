package dk.ashlan.agent.memory;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MemoryAwareAgentOrchestrator {
    private final AgentOrchestrator orchestrator;

    public MemoryAwareAgentOrchestrator(AgentOrchestrator orchestrator, MemoryService memoryService) {
        this.orchestrator = orchestrator;
    }

    public AgentRunResult run(String sessionId, String message) {
        return orchestrator.run(message, sessionId);
    }
}
