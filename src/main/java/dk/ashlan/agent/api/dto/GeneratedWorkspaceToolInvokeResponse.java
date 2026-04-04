package dk.ashlan.agent.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record GeneratedWorkspaceToolInvokeResponse(
        @Schema(description = "Session identifier.")
        String sessionId,
        @Schema(description = "Workspace identifier derived from the session id.")
        String workspaceId,
        @Schema(description = "Generated tool name.")
        String toolName,
        @Schema(description = "Whether the generated tool invocation succeeded.")
        boolean success,
        @Schema(description = "Stable text output from the generated tool.")
        String output,
        @Schema(description = "Structured generated-tool data.")
        Map<String, Object> data,
        @Schema(description = "Stable Chapter 8 trace markers captured for the session.")
        List<String> traceMarkers
) {
    public GeneratedWorkspaceToolInvokeResponse {
        data = data == null ? Map.of() : new LinkedHashMap<>(data);
    }
}
