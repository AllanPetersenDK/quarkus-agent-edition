package dk.ashlan.agent.chapters.chapter07;

import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.AgentRunner;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.planning.ExecutionPlan;
import dk.ashlan.agent.planning.PlannedAgentOrchestrator;
import dk.ashlan.agent.planning.PlannerService;
import dk.ashlan.agent.planning.ReflectionResult;
import dk.ashlan.agent.planning.ReflectionService;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class Chapter07Support {
    private Chapter07Support() {
    }

    static ExecutionPlan plan(String goal) {
        return new PlannerService().plan(goal);
    }

    static ReflectionResult reflect(String output) {
        return new ReflectionService().reflect(output);
    }

    static PlannedAgentOrchestrator orchestrator() {
        AtomicInteger attempts = new AtomicInteger();
        AgentRunner runner = goal -> {
            if (attempts.getAndIncrement() == 0) {
                return new AgentRunResult("short", StopReason.FINAL_ANSWER, 1, List.of("initial:" + goal));
            }
            return new AgentRunResult("This is a fuller answer for " + goal, StopReason.FINAL_ANSWER, 2, List.of("improved:" + goal));
        };
        return new PlannedAgentOrchestrator(new PlannerService(), new ReflectionService(), runner);
    }
}
