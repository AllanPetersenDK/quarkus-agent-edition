package dk.ashlan.agent.planning;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class PlannerService {
    public ExecutionPlan plan(String goal) {
        String normalized = goal == null ? "" : goal.toLowerCase(Locale.ROOT);
        if (normalized.contains("failure") || normalized.contains("retry") || normalized.contains("recover")) {
            return new ExecutionPlan(goal, List.of(
                    new PlanStep(1, "Inspect the failure or blocker", TaskStatus.COMPLETED, "The problem is visible and named.", "Start from the error, not the symptom."),
                    new PlanStep(2, "Choose a safer alternative", TaskStatus.IN_PROGRESS, "A correction or fallback exists.", "Switch method if the current one fails."),
                    new PlanStep(3, "Retry with the revised approach", TaskStatus.PENDING, "The revised step has been executed successfully.", "Stop repeating the same failure.")
            ));
        }
        if (normalized.contains("research") || normalized.contains("compare") || normalized.contains("multi-step") || normalized.contains("chapter 7")) {
            return new ExecutionPlan(goal, List.of(
                    new PlanStep(1, "Clarify the goal and constraints", TaskStatus.COMPLETED, "The request is specific enough to act on.", "Make the task small and concrete."),
                    new PlanStep(2, "Gather the relevant evidence", TaskStatus.IN_PROGRESS, "The needed facts or sources are available.", "Use tools or memory if they help."),
                    new PlanStep(3, "Synthesize the findings", TaskStatus.PENDING, "The answer can be supported from the evidence.", "Keep the final answer short and useful.")
            ));
        }
        return new ExecutionPlan(goal, List.of(
                new PlanStep(1, "Understand the request", TaskStatus.COMPLETED, "The request has been parsed.", ""),
                new PlanStep(2, "Use tools or knowledge if needed", TaskStatus.IN_PROGRESS, "The answer needs one more step.", "Prefer the simplest useful next action."),
                new PlanStep(3, "Produce the answer", TaskStatus.PENDING, "The answer is grounded in the gathered evidence.", "")
        ));
    }
}
