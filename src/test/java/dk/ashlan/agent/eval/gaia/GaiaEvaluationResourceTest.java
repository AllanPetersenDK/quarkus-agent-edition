package dk.ashlan.agent.eval.gaia;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import dk.ashlan.agent.api.GaiaEvaluationResource;
import io.smallrye.config.SmallRyeConfigBuilder;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GaiaEvaluationResourceTest {
    @TempDir
    Path tempDir;

    @Test
    void runAndLookupEndpointsExposeStoredResults() throws Exception {
        Path snapshotRoot = tempDir.resolve("snapshot");
        Path validationDir = snapshotRoot.resolve("2023/validation");
        Files.createDirectories(validationDir);
        GaiaTestSupport.writeParquet(validationDir.resolve("metadata.level1.parquet"), List.of(
                new GaiaTestSupport.GaiaRow("task-1", "What is PostgreSQL?", "PostgreSQL", "1", "", "")
        ));

        GaiaValidationRunner runner = new GaiaValidationRunner(
                agentOrchestrator(),
                new GaiaDatasetService(new GaiaParquetLoader(new com.fasterxml.jackson.databind.ObjectMapper(), new SmallRyeConfigBuilder().build()), new GaiaAttachmentResolver()),
                new GaiaEvalCaseMapper(),
                new GaiaAnswerScorer(),
                new GaiaAttachmentResolver(),
                new GaiaEvaluationStore(),
                new SmallRyeConfigBuilder().withDefaultValue("gaia.validation.default-config", "2023").withDefaultValue("gaia.validation.default-split", "validation").build()
        );
        GaiaEvaluationResource resource = new GaiaEvaluationResource(runner);

        GaiaRunResult runResult = resource.run(new GaiaRunRequest("", snapshotRoot.toString(), "", "", 1, 10, false));

        assertEquals(1, runResult.total());
        assertEquals(runResult.runId(), resource.runLookup(runResult.runId()).runId());
        assertEquals("task-1", resource.task("task-1").taskId());
        assertThrows(NotFoundException.class, () -> resource.task("missing-task"));
        assertThrows(NotFoundException.class, () -> resource.runLookup("missing-run"));
    }

    private AgentOrchestrator agentOrchestrator() {
        LlmClient client = (messages, toolRegistry, context) -> new LlmCompletion("PostgreSQL", List.of());
        ToolRegistry toolRegistry = new ToolRegistry(List.of());
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        return new AgentOrchestrator(client, toolRegistry, toolExecutor, null, null, 3, "");
    }
}
