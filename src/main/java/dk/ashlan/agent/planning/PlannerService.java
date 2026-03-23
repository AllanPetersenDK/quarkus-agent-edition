package dk.ashlan.agent.planning;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class PlannerService {
    public ExecutionPlan plan(String goal) {
        return new ExecutionPlan(goal, List.of(
                new PlanStep(1, "Understand the request", true),
                new PlanStep(2, "Use tools or knowledge if needed", true),
                new PlanStep(3, "Produce the answer", false)
        ));
    }
}
