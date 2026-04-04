package dk.ashlan.agent.eval.gaia;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class GaiaEvaluationResourceQuarkusTest {
    @TempDir
    Path tempDir;

    @Test
    void gaiaRunAndLookupWorkThroughHttp() throws Exception {
        Path snapshotRoot = tempDir.resolve("snapshot");
        Path validationDir = snapshotRoot.resolve("2023/validation");
        Files.createDirectories(validationDir);
        GaiaTestSupport.writeParquet(validationDir.resolve("metadata.level1.parquet"), List.of(
                new GaiaTestSupport.GaiaRow("task-http-1", "What is PostgreSQL?", "Direct answer: What is PostgreSQL?", "1", "", "")
        ));

        GaiaRunResult runResult = given()
                .contentType(ContentType.JSON)
                .body(new GaiaRunRequest("", snapshotRoot.toString(), "2023", "validation", 1, 10, false))
                .when()
                .post("/admin/evaluations/gaia/run")
                .then()
                .statusCode(200)
                .body("total", equalTo(1))
                .body("results[0].taskId", equalTo("task-http-1"))
                .body("results[0].predictedAnswer", notNullValue())
                .extract()
                .as(GaiaRunResult.class);

        given()
                .when()
                .get("/admin/evaluations/gaia/{taskId}", "task-http-1")
                .then()
                .statusCode(200)
                .body("taskId", equalTo("task-http-1"))
                .body("predictedAnswer", equalTo(runResult.results().getFirst().predictedAnswer()));

        given()
                .when()
                .get("/admin/evaluations/gaia/runs/{runId}", runResult.runId())
                .then()
                .statusCode(200)
                .body("runId", equalTo(runResult.runId()))
                .body("total", equalTo(1));

        given()
                .when()
                .get("/admin/evaluations/gaia/{taskId}", "missing-task")
                .then()
                .statusCode(404);
    }
}
