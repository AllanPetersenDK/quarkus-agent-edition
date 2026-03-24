package dk.ashlan.agent.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class OpenApiSupportTest {
    @Test
    void openApiEndpointExposesTheAgentApiContract() {
        given()
                .when()
                .get("/openapi")
                .then()
                .statusCode(200)
                .body(containsString("/api/agent/run"))
                .body(containsString("/api/agent/tools"))
                .body(containsString("Quarkus Agent Edition API"));
    }

    @Test
    void swaggerUiIsServedOnTheConfiguredPath() {
        given()
                .when()
                .get("/swagger-ui")
                .then()
                .statusCode(200)
                .body(containsString("Swagger UI"))
                .body(containsString("swagger-ui"));
    }
}
