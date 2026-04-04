package dk.ashlan.agent.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record CodeAgentRunRequest(
        @NotBlank
        @Schema(description = "Session identifier used to scope the Chapter 8 workspace.", required = true, examples = {"chapter8-demo"})
        String sessionId,
        @NotBlank
        @Schema(description = "Prompt or request that should guide the constrained code-agent flow.", required = true, examples = {"Generate a workspace helper"})
        String message
) {
}
