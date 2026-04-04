package dk.ashlan.agent.core.callback;

import dk.ashlan.agent.core.AfterToolContext;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.tools.JsonToolResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolResultCompressionCallbackTest {
    @Test
    void truncatesLargeOutputsForDocumentLikeTools() {
        ToolResultCompressionCallback callback = new ToolResultCompressionCallback();
        String largeOutput = "x".repeat(2500);
        JsonToolResult compressed = callback.afterTool(new AfterToolContext(
                "session-1",
                1,
                new LlmToolCall("read_document_file", Map.of()),
                JsonToolResult.success("read_document_file", largeOutput)
        ));

        assertEquals("read_document_file", compressed.toolName());
        assertTrue(compressed.output().contains("truncated"));
        assertTrue(Boolean.TRUE.equals(compressed.data().get("truncated")));
        assertEquals("generic-truncation", compressed.data().get("compressionStrategy"));
    }

    @Test
    void leavesSmallOutputsUntouched() {
        ToolResultCompressionCallback callback = new ToolResultCompressionCallback();
        JsonToolResult original = JsonToolResult.success("calculator", "100");
        JsonToolResult compressed = callback.afterTool(new AfterToolContext(
                "session-1",
                1,
                new LlmToolCall("calculator", Map.of()),
                original
        ));

        assertEquals(original, compressed);
    }
}
