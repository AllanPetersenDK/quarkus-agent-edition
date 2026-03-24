package dk.ashlan.agent.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
class ToolResourceTest {
    @Test
    void toolEndpointListsRegisteredTools() {
        given()
                .when()
                .get("/api/agent/tools")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("name", hasItem("calculator"))
                .body("find { it.name == 'calculator' }.description", equalTo("Evaluate a simple arithmetic expression."));
    }
}
