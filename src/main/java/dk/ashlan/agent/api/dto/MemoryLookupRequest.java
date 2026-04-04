package dk.ashlan.agent.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record MemoryLookupRequest(
        @NotBlank
        @Schema(
                description = "Session identifier for the memory lookup.",
                required = true,
                examples = {"chapter6-pattern3-tool"}
        )
        String sessionId,
        @NotBlank
        @Schema(
                description = "Query used to search the memory store.",
                required = true,
                examples = {"PostgreSQL"}
        )
        String query
) {
    public MemoryLookupRequest {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "default";
        }
        if (query == null) {
            query = "";
        }
    }
}
