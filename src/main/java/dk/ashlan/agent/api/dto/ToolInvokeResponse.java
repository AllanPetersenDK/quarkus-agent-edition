package dk.ashlan.agent.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;

public record ToolInvokeResponse(
        @Schema(description = "Runtime tool name.")
        String toolName,
        @Schema(description = "Whether the tool call succeeded.")
        boolean success,
        @Schema(description = "Tool output rendered as a stable string.")
        String output,
        @Schema(description = "Tool-specific structured data.")
        Map<String, Object> data,
        @Schema(description = "Session identifier forwarded to the tool, if any.")
        String sessionId,
        @Schema(description = "Error message when the tool call failed.", nullable = true)
        String error
) {
    public ToolInvokeResponse {
        data = data == null ? Map.of() : new LinkedHashMap<>(data);
    }
}
