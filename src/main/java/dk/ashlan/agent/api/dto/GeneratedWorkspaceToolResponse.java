package dk.ashlan.agent.api.dto;

import dk.ashlan.agent.code.GeneratedWorkspaceTool;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record GeneratedWorkspaceToolResponse(
        @Schema(description = "Generated tool name.")
        String name,
        @Schema(description = "Generated tool description.")
        String description,
        @Schema(description = "Prompt that produced the generated tool.")
        String prompt,
        @Schema(description = "Workspace-relative skill card path.")
        String skillPath,
        @Schema(description = "Workspace artifacts used as the generated tool's source material.")
        List<String> sourceArtifacts,
        @Schema(description = "Creation timestamp.")
        Instant createdAt,
        @Schema(description = "Last invocation timestamp, if the tool has been invoked.")
        Instant lastInvokedAt,
        @Schema(description = "Invocation count for the generated tool.")
        int invocationCount
) {
    public static GeneratedWorkspaceToolResponse from(GeneratedWorkspaceTool tool) {
        return new GeneratedWorkspaceToolResponse(
                tool.name(),
                tool.description(),
                tool.prompt(),
                tool.skillPath(),
                tool.sourceArtifacts(),
                tool.createdAt(),
                tool.lastInvokedAt(),
                tool.invocationCount()
        );
    }
}
