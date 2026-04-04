package dk.ashlan.agent.api.dto;

import dk.ashlan.agent.code.GeneratedWorkspaceTool;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

public record GeneratedWorkspaceToolResponse(
        @Schema(description = "Generated tool name.")
        String name,
        @Schema(description = "Generated tool description.")
        String description,
        @Schema(description = "Prompt that produced the generated tool.")
        String prompt,
        @Schema(description = "Workspace-relative skill card path.")
        String skillPath,
        @Schema(description = "Creation timestamp.")
        Instant createdAt
) {
    public static GeneratedWorkspaceToolResponse from(GeneratedWorkspaceTool tool) {
        return new GeneratedWorkspaceToolResponse(tool.name(), tool.description(), tool.prompt(), tool.skillPath(), tool.createdAt());
    }
}
