package dk.ashlan.agent.eval;

import java.util.List;

public record AgentTrace(String caseId, List<String> events) {
}
