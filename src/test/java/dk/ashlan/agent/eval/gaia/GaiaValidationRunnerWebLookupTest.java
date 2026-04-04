package dk.ashlan.agent.eval.gaia;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import dk.ashlan.agent.tools.WebSearchTool;
import dk.ashlan.agent.tools.OpenAiWebSearchService;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaiaValidationRunnerWebLookupTest {
    @TempDir
    Path tempDir;

    @Test
    void webSearchToolCanBeUsedDuringGaiaValidationRuns() throws Exception {
        Path snapshotRoot = tempDir.resolve("snapshot");
        Path validationDir = snapshotRoot.resolve("2023/validation");
        Files.createDirectories(validationDir);
        GaiaTestSupport.writeParquet(validationDir.resolve("metadata.level1.parquet"), List.of(
                new GaiaTestSupport.GaiaRow("task-web-1", "What current web fact should be found?", "The current web fact is 42.", "1", "", "")
        ));

        WebSearchTool webSearchTool = new WebSearchTool(query -> new OpenAiWebSearchService.WebSearchResult(
                "The current web fact is 42.",
                List.of(new OpenAiWebSearchService.WebSearchSource("Example source", "https://example.com/fact"))
        ));
        ToolRegistry toolRegistry = new ToolRegistry(List.of(webSearchTool));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        AtomicInteger calls = new AtomicInteger();
        LlmClient client = (messages, registry, context) -> {
            if (calls.getAndIncrement() == 0) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("web-search", Map.of("query", context.getInput()), "call-web-1")));
            }
            return LlmCompletion.answer("The current web fact is 42.");
        };

        AgentOrchestrator orchestrator = new AgentOrchestrator(client, toolRegistry, toolExecutor, null, null, 3, "");
        GaiaValidationRunner runner = new GaiaValidationRunner(
                orchestrator,
                new GaiaDatasetService(new GaiaParquetLoader(new com.fasterxml.jackson.databind.ObjectMapper(), new SmallRyeConfigBuilder().build()), new GaiaAttachmentResolver(path -> {
                    throw new AssertionError("audio transcription should not be used in web lookup test");
                })),
                new GaiaEvalCaseMapper(),
                new GaiaAnswerScorer(),
                new GaiaAttachmentResolver(path -> {
                    throw new AssertionError("audio transcription should not be used in web lookup test");
                }),
                new GaiaEvaluationStore(),
                new SmallRyeConfigBuilder().withDefaultValue("gaia.validation.default-config", "2023").withDefaultValue("gaia.validation.default-split", "validation").build()
        );

        GaiaRunResult result = runner.run(new GaiaRunRequest("", snapshotRoot.toString(), "", "", 1, 10, false));

        assertEquals(1, result.total());
        assertEquals(1, result.passed());
        assertEquals(0, result.failed());
        assertTrue(result.results().getFirst().trace().stream().anyMatch(event -> event.contains("tool:web-search")));
        assertTrue(result.results().getFirst().trace().stream().anyMatch(event -> event.contains("answer:The current web fact is 42.")));
        assertEquals(StopReason.FINAL_ANSWER.name(), result.results().getFirst().stopReason());
    }
}
