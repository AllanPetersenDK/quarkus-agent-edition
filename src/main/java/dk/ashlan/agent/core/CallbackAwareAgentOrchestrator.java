package dk.ashlan.agent.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CallbackAwareAgentOrchestrator implements AgentRunner {
    private final AgentOrchestrator orchestrator;
    private final List<String> callbacks = new CopyOnWriteArrayList<>();

    public CallbackAwareAgentOrchestrator(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public AgentRunResult run(String message) {
        callbacks.add("before:" + message);
        AgentRunResult result = orchestrator.run(message);
        callbacks.add("after:" + result.stopReason());
        return result;
    }

    public List<String> callbacks() {
        return List.copyOf(callbacks);
    }
}
