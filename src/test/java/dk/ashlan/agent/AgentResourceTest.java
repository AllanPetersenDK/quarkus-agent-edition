package dk.ashlan.agent;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class AgentResourceTest {
    @Test
    void agentEndpointUsesCalculatorTool() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"message\":\"What is 25 * 4?\"}")
                .when()
                .post("/agent")
                .then()
                .statusCode(200)
                .body("stopReason", equalTo("FINAL_ANSWER"))
                .body("finalAnswer", containsString("100"));
    }
}
