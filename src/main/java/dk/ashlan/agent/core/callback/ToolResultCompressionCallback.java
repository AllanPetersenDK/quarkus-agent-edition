package dk.ashlan.agent.core.callback;

import dk.ashlan.agent.core.AfterToolContext;
import dk.ashlan.agent.tools.JsonToolResult;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
@Priority(100)
public class ToolResultCompressionCallback implements AgentCallback {
    private static final Set<String> COMPRESSIBLE_TOOLS = Set.of(
            "web-search",
            "list_files",
            "read_file",
            "read_document_file",
            "read_media_file"
    );
    private static final int MAX_OUTPUT_CHARS = 1400;

    @Override
    public JsonToolResult afterTool(AfterToolContext context) {
        JsonToolResult result = context.toolResult();
        if (result == null || !result.success()) {
            return result;
        }
        String toolName = context.toolCall() == null ? result.toolName() : context.toolCall().toolName();
        if (!COMPRESSIBLE_TOOLS.contains(toolName)) {
            return result;
        }
        String output = result.output();
        if (output == null || output.length() <= MAX_OUTPUT_CHARS) {
            return result;
        }
        String trimmed = output.substring(0, MAX_OUTPUT_CHARS).stripTrailing();
        Map<String, Object> data = new LinkedHashMap<>(result.data());
        data.put("truncated", true);
        data.put("originalLength", output.length());
        data.put("compressedLength", trimmed.length());
        data.put("compressionNote", "Output truncated for callback compression.");
        String compressedOutput = trimmed + "\n...[truncated " + (output.length() - trimmed.length()) + " chars]";
        return new JsonToolResult(toolName, true, compressedOutput, Map.copyOf(data));
    }
}
