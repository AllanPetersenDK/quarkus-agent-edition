package dk.ashlan.agent.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class CodeAgentResourceTest {
    @Test
    void codeAgentEndpointReturnsGeneratedWorkspaceOutput() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"message\":\"Generate a response file\"}")
                .when()
                .post("/code-agent")
                .then()
                .statusCode(200)
                .body("testExitCode", equalTo(0))
                .body("response", containsString("Workspace:"))
                .body("testOutput", containsString("placeholder"));
    }
}
