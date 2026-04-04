package dk.ashlan.agent.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;

public record ToolInvokeRequest(
        @NotBlank
        @Schema(
                description = "Runtime tool name to invoke through the chapter-3 direct tool seam.",
                required = true,
                examples = {"calculator"}
        )
        String toolName,
        @Schema(
                description = "Tool arguments as a simple JSON object.",
                required = true
        )
        Map<String, Object> arguments,
        @Schema(
                description = "Optional session identifier forwarded to session-aware tools.",
                defaultValue = "default",
                examples = {"default"}
        )
        String sessionId
) {
    public ToolInvokeRequest {
        arguments = arguments == null ? Map.of() : new LinkedHashMap<>(arguments);
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "default";
        }
    }
}
