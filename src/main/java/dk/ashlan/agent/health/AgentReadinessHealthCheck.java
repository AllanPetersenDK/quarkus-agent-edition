package dk.ashlan.agent.health;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.tools.ToolRegistry;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Readiness
@ApplicationScoped
public class AgentReadinessHealthCheck implements HealthCheck {
    private final AgentOrchestrator orchestrator;
    private final ToolRegistry toolRegistry;

    @Inject
    public AgentReadinessHealthCheck(AgentOrchestrator orchestrator, ToolRegistry toolRegistry) {
        this.orchestrator = orchestrator;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public HealthCheckResponse call() {
        int toolCount = toolRegistry.tools().size();
        var builder = HealthCheckResponse.named("agent-runtime");
        builder.withData("orchestrator", orchestrator.getClass().getSimpleName());
        builder.withData("toolCount", toolCount);
        if (toolCount == 0) {
            return builder.down().build();
        }
        return builder.up().build();
    }
}
