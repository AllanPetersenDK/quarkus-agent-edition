package dk.ashlan.agent.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
class AdminEvaluationResourceTest {
    @Test
    void evaluationEndpointReturnsResultsAndMetrics() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        [
                          {"id":"calc","prompt":"What is 25 * 4?","expectedSubstring":"100"}
                        ]
                        """)
                .when()
                .post("/admin/evaluations")
        .then()
                .statusCode(200)
                .body("metrics.total", equalTo(1))
                .body("metrics.passed", equalTo(1))
                .body("metrics.failed", equalTo(0))
                .body("metrics.durationMillis", greaterThanOrEqualTo(0));
    }
}
