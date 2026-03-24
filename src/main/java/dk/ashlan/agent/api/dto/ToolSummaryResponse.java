package dk.ashlan.agent.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ToolSummaryResponse(
        @Schema(description = "Tool name.")
        String name,
        @Schema(description = "Human-readable tool description.")
        String description
) {
}
