package dk.ashlan.agent.eval.gaia;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaiaValidationRunnerTest {
    @TempDir
    Path tempDir;

    @Test
    void runnerStoresPerTaskTraceAndUsesAttachmentContext() throws Exception {
        Path snapshotRoot = tempDir.resolve("snapshot");
        Path validationDir = snapshotRoot.resolve("2023/validation");
        Files.createDirectories(validationDir);
        Files.writeString(validationDir.resolve("attachment.txt"), "GAIA attachment content");
        GaiaTestSupport.writeParquet(validationDir.resolve("metadata.level1.parquet"), List.of(
                new GaiaTestSupport.GaiaRow("task-1", "What is PostgreSQL?", "PostgreSQL", "1", "attachment.txt", "attachment.txt")
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

        GaiaRunResult result = runner.run(new GaiaRunRequest("", snapshotRoot.toString(), "", "", 1, 10, false));

        assertEquals(1, result.total());
        assertEquals(1, result.passed());
        assertEquals(0, result.failed());
        assertEquals("task-1", result.results().getFirst().taskId());
        assertTrue(result.results().getFirst().trace().stream().anyMatch(event -> event.startsWith("attachment:present")));
        assertTrue(runner.trace("task-1").trace().stream().anyMatch(event -> event.contains("scoreReason")));
    }

    private AgentOrchestrator agentOrchestrator() {
        LlmClient client = (messages, toolRegistry, context) -> new LlmCompletion("PostgreSQL", List.of());
        ToolRegistry toolRegistry = new ToolRegistry(List.of());
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        return new AgentOrchestrator(client, toolRegistry, toolExecutor, null, null, 3, "");
    }
}
