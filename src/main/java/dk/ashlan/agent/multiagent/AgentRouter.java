package dk.ashlan.agent.multiagent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Comparator;

@ApplicationScoped
public class AgentRouter {
    private final List<SpecialistAgent> specialists;

    @Inject
    public AgentRouter(Instance<SpecialistAgent> specialists) {
        this.specialists = normalize(specialists.stream().toList());
    }

    public AgentRouter(List<SpecialistAgent> specialists) {
        this.specialists = normalize(specialists);
    }

    public SpecialistAgent route(AgentTask task) {
        return routeDecision(task).specialist();
    }

    public RoutingDecision routeDecision(AgentTask task) {
        return specialists.stream()
                .filter(agent -> !"reviewer".equals(agent.name()))
                .filter(agent -> agent.supports(task))
                .findFirst()
                .map(agent -> new RoutingDecision(agent, reasonFor(agent, task, false)))
                .orElseGet(() -> specialists.stream()
                        .filter(agent -> !"reviewer".equals(agent.name()))
                        .findFirst()
                        .map(agent -> new RoutingDecision(agent, reasonFor(agent, task, true)))
                        .orElseThrow(() -> new IllegalStateException("No specialist available")));
    }

    private String reasonFor(SpecialistAgent specialist, AgentTask task, boolean fallback) {
        if (fallback) {
            return "fallback to " + specialist.name() + " because no clear specialist keyword was found";
        }
        String objective = task.objective().toLowerCase();
        if ("research".equals(specialist.name())) {
            return objective.contains("research") || objective.contains("find") || objective.contains("investigate")
                    ? "objective contains research/finding keywords"
                    : "objective best matched research specialist";
        }
        if ("coding".equals(specialist.name())) {
            return objective.contains("code") || objective.contains("implement") || objective.contains("routing")
                    ? "objective contains coding/implementation keywords"
                    : "objective best matched coding specialist";
        }
        return "selected " + specialist.name();
    }

    private List<SpecialistAgent> normalize(List<SpecialistAgent> specialists) {
        return specialists.stream()
                .sorted(Comparator.comparingInt(this::priority).thenComparing(SpecialistAgent::name))
                .toList();
    }

    private int priority(SpecialistAgent specialist) {
        return switch (specialist.name()) {
            case "research" -> 0;
            case "coding" -> 1;
            case "reviewer" -> 99;
            default -> 50;
        };
    }
}
