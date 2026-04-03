package dk.ashlan.agent.memory;

import dk.ashlan.agent.core.AgentStepResult;

import java.util.List;
import java.util.Optional;

public interface SessionTraceStore {
    Optional<List<AgentStepResult>> load(String sessionId);

    void append(AgentStepResult stepResult);
}
