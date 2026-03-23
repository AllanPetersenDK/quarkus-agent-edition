package dk.ashlan.agent.multiagent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class AgentRouter {
    private final List<SpecialistAgent> specialists;

    @Inject
    public AgentRouter(Instance<SpecialistAgent> specialists) {
        this.specialists = specialists.stream().toList();
    }

    public AgentRouter(List<SpecialistAgent> specialists) {
        this.specialists = specialists;
    }

    public SpecialistAgent route(AgentTask task) {
        return specialists.stream()
                .filter(agent -> !"reviewer".equals(agent.name()))
                .filter(agent -> agent.supports(task))
                .findFirst()
                .orElseGet(() -> specialists.stream()
                        .filter(agent -> !"reviewer".equals(agent.name()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No specialist available")));
    }
}
