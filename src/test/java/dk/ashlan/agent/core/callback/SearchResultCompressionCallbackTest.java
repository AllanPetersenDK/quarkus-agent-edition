package dk.ashlan.agent.core.callback;

import dk.ashlan.agent.core.AfterToolContext;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.tools.JsonToolResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchResultCompressionCallbackTest {
    @Test
    void longWebSearchOutputIsCompressedByQueryRelevance() {
        ToolResultCompressionCallback callback = new ToolResultCompressionCallback();
        String output = """
                BBC Earth video result about wildlife and birds.

                A different result about space telescopes and stars.

                Rockhopper penguin featured in the BBC Earth clip.

                Generic note about penguins in general.

                Another unrelated page about cooking and recipes.
                """;

        JsonToolResult compressed = callback.afterTool(new AfterToolContext(
                "session-1",
                1,
                new LlmToolCall("web-search", Map.of("query", "BBC Earth rockhopper penguin")),
                JsonToolResult.success("web-search", output.repeat(40))
        ));

        assertEquals("web-search", compressed.toolName());
        assertTrue(compressed.output().contains("Search query: BBC Earth rockhopper penguin"));
        assertTrue(
                compressed.output().contains("BBC Earth video result about wildlife and birds.")
                        || compressed.output().contains("Rockhopper penguin featured in the BBC Earth clip.")
        );
        assertFalse(compressed.output().contains("cooking and recipes"));
        assertEquals("search-query-ranking", compressed.data().get("compressionStrategy"));
        assertTrue(Boolean.TRUE.equals(compressed.data().get("compressed")));
        assertTrue(((Number) compressed.data().get("selectedChunkCount")).intValue() >= 1);
        assertTrue(((Number) compressed.data().get("originalLength")).intValue() > ((Number) compressed.data().get("compressedLength")).intValue());
    }

    @Test
    void shortWebSearchOutputRemainsUnchanged() {
        ToolResultCompressionCallback callback = new ToolResultCompressionCallback();
        JsonToolResult original = JsonToolResult.success("web-search", "Short search result");

        JsonToolResult compressed = callback.afterTool(new AfterToolContext(
                "session-1",
                1,
                new LlmToolCall("web-search", Map.of("query", "who is Ada")),
                original
        ));

        assertEquals(original, compressed);
    }
}
