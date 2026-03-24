package dk.ashlan.agent.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

public record AgentRunRequest(
        @NotBlank
        @Schema(
                description = "The user message to send to the agent.",
                required = true,
                examples = {"What is 25 * 4?"}
        )
        String message,
        @Schema(
                description = "Conversation session identifier used for memory lookup.",
                defaultValue = "default",
                examples = {"default"}
        )
        String sessionId
) {
    public AgentRunRequest {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "default";
        }
    }
}
