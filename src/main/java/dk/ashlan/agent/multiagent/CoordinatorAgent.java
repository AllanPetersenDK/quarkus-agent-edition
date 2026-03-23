package dk.ashlan.agent.multiagent;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CoordinatorAgent {
    private final AgentRouter router;
    private final ReviewerAgent reviewerAgent;

    public CoordinatorAgent(AgentRouter router, ReviewerAgent reviewerAgent) {
        this.router = router;
        this.reviewerAgent = reviewerAgent;
    }

    public AgentTaskResult run(String objective) {
        AgentTask task = new AgentTask("task-1", objective, objective);
        SpecialistAgent specialist = router.route(task);
        AgentTaskResult draft = specialist.execute(task);
        AgentTaskResult review = reviewerAgent.execute(new AgentTask(task.id(), task.objective(), draft.output()));
        return new AgentTaskResult(specialist.name(), draft.output(), review.approved(), review.review());
    }
}
