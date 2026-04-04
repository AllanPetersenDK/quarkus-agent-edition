package dk.ashlan.agent.api.dto;

import dk.ashlan.agent.core.ToolConfirmation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;

import java.util.List;

public record AgentRunRequest(
        @Schema(
                description = "The user message to send to the agent. Optional when resuming with tool confirmations.",
                nullable = true,
                examples = {"What is 25 * 4?"}
        )
        String message,
        @Schema(
                description = "Conversation session identifier used for memory lookup. Leave blank for an anonymous ephemeral run.",
                nullable = true
        )
        String sessionId,
        @Schema(
                description = "Optional whitelist of tool confirmations used to resume a pending chapter-6 run.",
                nullable = true
        )
        List<ToolConfirmation> toolConfirmations
) {
    public AgentRunRequest(String message, String sessionId) {
        this(message, sessionId, List.of());
    }

    public AgentRunRequest {
        toolConfirmations = toolConfirmations == null ? List.of() : List.copyOf(toolConfirmations);
    }

    @AssertTrue(message = "Either message or toolConfirmations must be supplied.")
    public boolean isMessageOrConfirmationsPresent() {
        return (message != null && !message.isBlank()) || !toolConfirmations.isEmpty();
    }
}
