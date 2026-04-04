package dk.ashlan.agent.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record CodeWorkspaceInspectionResponse(
        @Schema(description = "Session identifier.")
        String sessionId,
        @Schema(description = "Workspace identifier derived from the session id.")
        String workspaceId,
        @Schema(description = "Most recent Chapter 8 run identifier for the session.", nullable = true)
        String lastRunId,
        @Schema(description = "Absolute workspace root path.")
        String workspaceRoot,
        @Schema(description = "Workspace creation timestamp.")
        Instant createdAt,
        @Schema(description = "Last update timestamp.")
        Instant updatedAt,
        @Schema(description = "Number of files currently present in the workspace.")
        long fileCount,
        @Schema(description = "Number of generated tools registered for the session.")
        int generatedToolCount,
        @Schema(description = "Last request recorded for the session.", nullable = true)
        String lastRequest,
        @Schema(description = "Stable Chapter 8 trace markers captured for the session.")
        List<String> traceMarkers
) {
}
