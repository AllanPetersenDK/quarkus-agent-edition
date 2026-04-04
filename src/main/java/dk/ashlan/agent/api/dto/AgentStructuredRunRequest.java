package dk.ashlan.agent.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record AgentStructuredRunRequest(
        @NotBlank
        @Schema(
                description = "The user message to send through the chapter-4 structured-output seam.",
                required = true,
                examples = {"Answer in a single sentence."}
        )
        String message,
        @Schema(
                description = "Conversation session identifier used for the runtime step and trace seams. Leave blank for an anonymous ephemeral run.",
                nullable = true
        )
        String sessionId,
        @Schema(
                description = "Controlled chapter-4 structured-output mode. Only `chapter4-answer` is supported.",
                defaultValue = "chapter4-answer",
                examples = {"chapter4-answer"}
        )
        String mode
) {
    public AgentStructuredRunRequest {
        if (mode == null || mode.isBlank()) {
            mode = "chapter4-answer";
        }
    }
}
