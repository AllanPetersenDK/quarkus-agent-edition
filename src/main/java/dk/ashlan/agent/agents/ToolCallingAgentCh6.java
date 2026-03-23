package dk.ashlan.agent.agents;

import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.memory.MemoryAwareAgentOrchestrator;

public class ToolCallingAgentCh6 {
    private final MemoryAwareAgentOrchestrator orchestrator;

    public ToolCallingAgentCh6(MemoryAwareAgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public AgentRunResult run(String sessionId, String message) {
        return orchestrator.run(sessionId, message);
    }
}
