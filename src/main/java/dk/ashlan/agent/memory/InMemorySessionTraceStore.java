package dk.ashlan.agent.memory;

import dk.ashlan.agent.core.AgentStepResult;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@DefaultBean
@ApplicationScoped
public class InMemorySessionTraceStore implements SessionTraceStore {
    private final Map<String, List<AgentStepResult>> traces = new ConcurrentHashMap<>();

    @Override
    public Optional<List<AgentStepResult>> load(String sessionId) {
        return Optional.ofNullable(traces.get(sessionId)).map(List::copyOf);
    }

    @Override
    public void append(AgentStepResult stepResult) {
        traces.compute(stepResult.sessionId(), (sessionId, existing) -> {
            List<AgentStepResult> updated = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            updated.add(stepResult);
            return List.copyOf(updated);
        });
    }
}
