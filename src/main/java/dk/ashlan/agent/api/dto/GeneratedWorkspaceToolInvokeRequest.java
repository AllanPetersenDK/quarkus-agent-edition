package dk.ashlan.agent.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;

public record GeneratedWorkspaceToolInvokeRequest(
        @NotBlank
        @Schema(description = "Session-scoped generated tool name.", required = true, examples = {"workspace-summary"})
        String toolName,
        @Schema(description = "Optional JSON arguments for the generated tool.", required = true)
        Map<String, Object> arguments
) {
    public GeneratedWorkspaceToolInvokeRequest {
        arguments = arguments == null ? Map.of() : new LinkedHashMap<>(arguments);
    }
}
