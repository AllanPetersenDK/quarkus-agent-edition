package dk.ashlan.agent.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class RuntimeLivenessHealthCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("runtime-liveness")
                .withData("component", "quarkus-agent-edition")
                .withData("surface", "swagger-visible runtime companion seams")
                .up()
                .build();
    }
}
