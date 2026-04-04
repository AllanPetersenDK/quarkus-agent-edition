package dk.ashlan.agent.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record MemoryLookupResponse(
        @Schema(description = "Tool or seam name used for the lookup.")
        String toolName,
        @Schema(description = "Session identifier used for the lookup.")
        String sessionId,
        @Schema(description = "Query used for the lookup.")
        String query,
        @Schema(description = "Compact output from the memory lookup.")
        String output
) {
}
