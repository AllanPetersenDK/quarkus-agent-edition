package dk.ashlan.agent.planning;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunner;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.StopReason;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PlannedAgentOrchestrator {
    private final PlannerService plannerService;
    private final ReflectionService reflectionService;
    private final AgentRunner agentRunner;

    public PlannedAgentOrchestrator(PlannerService plannerService, ReflectionService reflectionService, AgentRunner agentRunner) {
        this.plannerService = plannerService;
        this.reflectionService = reflectionService;
        this.agentRunner = agentRunner;
    }

    public AgentRunResult run(String goal) {
        plannerService.plan(goal);
        AgentRunResult initial = agentRunner.run(goal);
        ReflectionResult reflection = reflectionService.reflect(initial.finalAnswer());
        if (reflection.accepted()) {
            return initial;
        }
        AgentRunResult improved = agentRunner.run(goal + " Please expand the answer and include more detail.");
        ReflectionResult improvedReflection = reflectionService.reflect(improved.finalAnswer());
        StopReason stopReason = improvedReflection.accepted() ? improved.stopReason() : StopReason.REFLECTION_REJECTED;
        return new AgentRunResult(improved.finalAnswer(), stopReason, improved.iterations(), improved.trace());
    }
}
