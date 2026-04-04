package dk.ashlan.agent.multiagent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class CoordinatorAgent {
    private final AgentRouter router;
    private final ReviewerAgent reviewerAgent;
    private final Chapter9RunHistoryStore historyStore;

    public CoordinatorAgent(AgentRouter router, ReviewerAgent reviewerAgent) {
        this(router, reviewerAgent, new Chapter9RunHistoryStore());
    }

    @Inject
    public CoordinatorAgent(AgentRouter router, ReviewerAgent reviewerAgent, Chapter9RunHistoryStore historyStore) {
        this.router = router;
        this.reviewerAgent = reviewerAgent;
        this.historyStore = historyStore;
    }

    public AgentTaskResult run(String objective) {
        String runId = historyStore.nextRunId();
        AgentTask task = new AgentTask(runId, objective, objective);
        RoutingDecision decision = router.routeDecision(task);
        SpecialistAgent specialist = decision.specialist();
        AgentTaskResult draft = specialist.execute(task);
        AgentTaskResult review = reviewerAgent.execute(new AgentTask(task.id(), task.objective(), draft.output()));
        List<String> traceEntries = List.of(
                "chapter9-run-start:" + runId,
                "chapter9-route:" + specialist.name(),
                "chapter9-review:" + (review.approved() ? "approved" : "rejected"),
                "chapter9-run-complete:" + runId
        );
        String coordinatorSummary = "Coordinator created " + runId
                + ", routed to " + specialist.name()
                + ", and reviewer " + (review.approved() ? "approved" : "rejected")
                + " the specialist draft.";
        AgentTaskResult run = new AgentTaskResult(
                runId,
                Instant.now(),
                objective,
                specialist.name(),
                draft.output(),
                review.approved(),
                review.review(),
                decision.reason(),
                coordinatorSummary,
                traceEntries
        );
        return historyStore.record(run);
    }

    public List<AgentTaskResult> history() {
        return historyStore.list();
    }

    public AgentTaskResult findRun(String runId) {
        return historyStore.find(runId).orElse(null);
    }
}
