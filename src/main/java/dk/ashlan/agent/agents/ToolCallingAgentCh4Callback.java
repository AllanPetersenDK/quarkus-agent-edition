package dk.ashlan.agent.agents;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;

import java.util.ArrayList;
import java.util.List;

public class ToolCallingAgentCh4Callback extends ToolCallingAgentCh4Base {
    private final List<String> callbacks = new ArrayList<>();

    public ToolCallingAgentCh4Callback(AgentOrchestrator orchestrator) {
        super(orchestrator);
    }

    @Override
    public AgentRunResult run(String message) {
        callbacks.add("before:" + message);
        AgentRunResult result = super.run(message);
        callbacks.add("after:" + result.stopReason());
        return result;
    }

    public List<String> callbacks() {
        return List.copyOf(callbacks);
    }
}
