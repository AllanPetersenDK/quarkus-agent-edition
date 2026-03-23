package dk.ashlan.agent.memory;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MemoryAwareAgentOrchestrator {
    private final AgentOrchestrator orchestrator;
    private final MemoryService memoryService;

    public MemoryAwareAgentOrchestrator(AgentOrchestrator orchestrator, MemoryService memoryService) {
        this.orchestrator = orchestrator;
        this.memoryService = memoryService;
    }

    public AgentRunResult run(String sessionId, String message) {
        memoryService.remember(sessionId, "request", message);
        return orchestrator.run(message, sessionId);
    }
}
