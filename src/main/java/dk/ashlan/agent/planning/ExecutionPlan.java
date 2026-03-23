package dk.ashlan.agent.planning;

import java.util.List;

public record ExecutionPlan(String goal, List<PlanStep> steps) {
}
