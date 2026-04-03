package dk.ashlan.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSearchToolTest {
    @Test
    void definitionGuidesTheModelToUseWebSearchOnlyForRealLookupTasks() {
        WebSearchTool tool = new WebSearchTool(blankService());

        assertTrue(tool.definition().description().contains("OpenAI Responses API"));
        assertTrue(tool.definition().description().contains("current, external, or explicitly requested lookup tasks"));
        assertTrue(tool.definition().description().contains("Not for simple stable facts or common general knowledge"));
    }

    @Test
    void executeReturnsAControlledFailureWhenOpenAiApiKeyIsMissing() {
        WebSearchTool tool = new WebSearchTool(blankService());

        JsonToolResult result = tool.execute(Map.of("query", "What is the capital of France?"));

        assertFalse(result.success());
        assertTrue(result.output().contains("OpenAI API key is not configured for web-search"));
    }

    private OpenAiWebSearchService blankService() {
        return new OpenAiWebSearchService(
                "",
                "gpt-4.1-mini",
                "http://example.com/v1",
                10,
                (uri, apiKey, payload, timeout) -> {
                    throw new AssertionError("transport should not be called");
                },
                new ObjectMapper()
        );
    }
}
