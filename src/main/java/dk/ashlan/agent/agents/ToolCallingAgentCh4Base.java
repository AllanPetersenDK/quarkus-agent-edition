package dk.ashlan.agent.agents;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;

public class ToolCallingAgentCh4Base {
    private final AgentOrchestrator orchestrator;

    public ToolCallingAgentCh4Base(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public AgentRunResult run(String message) {
        return orchestrator.run(message);
    }
}
