package dk.ashlan.agent.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class MultiAgentResourceTest {
    @Test
    void multiAgentEndpointRoutesAndReviews() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"message\":\"research the Quarkus agent edition\"}")
                .when()
                .post("/multi-agent")
                .then()
                .statusCode(200)
                .body("agentName", equalTo("research"))
                .body("output", containsString("Research summary"))
                .body("approved", equalTo(true));
    }
}
