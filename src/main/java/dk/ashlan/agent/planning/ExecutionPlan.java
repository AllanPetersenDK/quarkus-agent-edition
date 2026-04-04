package dk.ashlan.agent.planning;

import java.util.List;

public record ExecutionPlan(String goal, List<PlanStep> steps) {
    public String formattedPlan() {
        StringBuilder builder = new StringBuilder("Task plan");
        if (goal != null && !goal.isBlank()) {
            builder.append("\nGoal: ").append(goal.trim());
        }
        if (steps != null) {
            for (PlanStep step : steps) {
                if (step != null) {
                    builder.append("\n").append(step.formattedLine());
                }
            }
        }
        return builder.toString();
    }

    public PlanStep nextActiveStep() {
        if (steps == null || steps.isEmpty()) {
            return null;
        }
        return steps.stream()
                .filter(step -> step != null && step.status() == TaskStatus.IN_PROGRESS)
                .findFirst()
                .orElse(steps.stream().filter(step -> step != null && step.status() == TaskStatus.PENDING).findFirst().orElse(steps.get(steps.size() - 1)));
    }
}
