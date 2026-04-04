package dk.ashlan.agent.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

public record CodeWorkspaceFilesResponse(
        @Schema(description = "Session identifier.")
        String sessionId,
        @Schema(description = "Workspace identifier derived from the session id.")
        String workspaceId,
        @Schema(description = "Absolute workspace root path.")
        String workspaceRoot,
        @Schema(description = "Workspace-relative file paths.")
        List<String> files,
        @Schema(description = "Stable Chapter 8 trace markers captured for the session.")
        List<String> traceMarkers
) {
}
