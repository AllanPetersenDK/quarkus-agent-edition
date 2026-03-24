package dk.ashlan.agent.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
class AgentHealthTest {
    @Test
    void readinessReportsTheAgentRuntimeAsUp() {
        given()
                .when()
                .get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.find { it.name == 'agent-runtime' }.status", equalTo("UP"))
                .body("checks.find { it.name == 'agent-runtime' }.data.toolCount", greaterThanOrEqualTo(1));
    }
}
