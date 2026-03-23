package dk.ashlan.agent.tools;

import java.util.Map;

public record JsonToolResult(
        String toolName,
        boolean success,
        String output,
        Map<String, Object> data
) {
    public static JsonToolResult success(String toolName, String output) {
        return new JsonToolResult(toolName, true, output, Map.of("output", output));
    }

    public static JsonToolResult failure(String toolName, String output) {
        return new JsonToolResult(toolName, false, output, Map.of("error", output));
    }
}
