package dk.ashlan.agent.api.dto;

import jakarta.validation.Valid;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

public record ContextOptimizeRequest(
        @Schema(
                description = "Messages to project through the chapter-6 request optimizer.",
                required = true
        )
        List<@Valid ContextOptimizeMessage> messages
) {
    public ContextOptimizeRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public record ContextOptimizeMessage(
            @Schema(description = "Message role.", examples = {"user", "assistant", "tool", "system"})
            String role,
            @Schema(description = "Message content.")
            String content,
            @Schema(description = "Optional message name.")
            String name,
            @Schema(description = "Optional tool-call identifier.")
            String toolCallId
    ) {
    }
}
