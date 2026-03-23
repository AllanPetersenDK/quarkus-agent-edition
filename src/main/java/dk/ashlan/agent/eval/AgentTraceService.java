package dk.ashlan.agent.eval;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class AgentTraceService {
    private final Map<String, AgentTrace> traces = new ConcurrentHashMap<>();

    public void record(String caseId, List<String> events) {
        traces.put(caseId, new AgentTrace(caseId, List.copyOf(events)));
    }

    public AgentTrace get(String caseId) {
        return traces.get(caseId);
    }
}
