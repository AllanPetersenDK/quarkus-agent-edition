package dk.ashlan.agent.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(ExternalSwaggerUsecasesTest.DemoRuntimeProfile.class)
class ExternalSwaggerUsecasesTest {
    @Test
    void openApiDocumentsTheChapterFiveAndSixUsecases() {
        String openApi = given()
                .accept(ContentType.JSON)
                .when()
                .get("/openapi")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertTrue(openApi.contains("\"/api/rag/ingest\""));
        assertTrue(openApi.contains("\"/api/rag/query\""));
        assertTrue(openApi.contains("\"/api/runtime/context/optimize\""));
        assertTrue(openApi.contains("\"/api/runtime/context/sliding-window\""));
        assertTrue(openApi.contains("\"/api/runtime/memory/recall\""));
        assertTrue(openApi.contains("\"/api/runtime/memory/conversation-search\""));
        assertTrue(openApi.contains("Book chapter: 5"));
        assertTrue(openApi.contains("Book chapter mapping: chapter 6 request-time context optimization inspection seam"));
        assertTrue(openApi.contains("Book chapter mapping: chapter 6 explicit long-term memory retrieval seam"));
    }

    @Test
    void chapterFiveRagIngestAndQueryWorkThroughSwaggerVisibleHttpEndpoints() {
        String sourceId = "chapter5-swagger-http";

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sourceId", sourceId,
                        "text", "PostgreSQL is an open-source relational database. Quarkus is a Java framework."
                ))
                .when()
                .post("/api/rag/ingest")
                .then()
                .statusCode(200)
                .body("sourceId", equalTo(sourceId))
                .body("chunkCount", greaterThan(0));

        given()
                .accept(ContentType.JSON)
                .queryParam("query", "Which text mentions PostgreSQL?")
                .queryParam("topK", 2)
                .when()
                .get("/api/rag/query")
                .then()
                .statusCode(200)
                .body("query", equalTo("Which text mentions PostgreSQL?"))
                .body("answer", containsString("PostgreSQL"))
                .body("bestChunk.sourceId", equalTo(sourceId))
                .body("citations[0].sourceId", equalTo(sourceId));
    }

    @Test
    void chapterSixContextOptimizationAndMemoryRecallWorkThroughSwaggerVisibleHttpEndpoints() {
        String sessionId = "chapter6-swagger-http";

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "message", "Remember that my favorite database is PostgreSQL.",
                        "sessionId", sessionId
                ))
                .when()
                .post("/api/agent/run")
                .then()
                .statusCode(200)
                .body("answer", containsString("Direct answer"))
                .body("stopReason", notNullValue())
                .body("pendingToolCalls.size()", equalTo(0));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "messages", List.of(
                                Map.of("role", "system", "content", "You are helpful."),
                                Map.of("role", "user", "content", "Hello"),
                                Map.of("role", "assistant", "content", "Acknowledged")
                        )
                ))
                .when()
                .post("/api/runtime/context/optimize")
                .then()
                .statusCode(200)
                .body("strategy", equalTo("none"))
                .body("changed", equalTo(false))
                .body("originalMessages.size()", equalTo(3))
                .body("projectedMessages.size()", equalTo(3));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sessionId", sessionId,
                        "query", "PostgreSQL"
                ))
                .when()
                .post("/api/runtime/memory/recall")
                .then()
                .statusCode(200)
                .body("toolName", equalTo("recall-memory"))
                .body("sessionId", equalTo(sessionId))
                .body("query", equalTo("PostgreSQL"))
                .body("output", containsString("PostgreSQL"));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sessionId", sessionId,
                        "query", "PostgreSQL"
                ))
                .when()
                .post("/api/runtime/memory/conversation-search")
                .then()
                .statusCode(200)
                .body("toolName", equalTo("conversation-search"))
                .body("output", containsString("Remember that my favorite database is PostgreSQL"));
    }

    public static final class DemoRuntimeProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "agent.llm-provider", "demo",
                    "quarkus.datasource.jdbc.url", "jdbc:h2:file:./target/agent-state/external-swagger-test;AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE"
            );
        }
    }
}
