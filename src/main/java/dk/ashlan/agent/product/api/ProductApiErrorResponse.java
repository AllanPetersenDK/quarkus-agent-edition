package dk.ashlan.agent.product.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Structured product API error response used for operator-friendly failures.")
public record ProductApiErrorResponse(
        @Schema(description = "Error timestamp.")
        Instant timestamp,
        @Schema(description = "HTTP status code.")
        int status,
        @Schema(description = "Stable error code.")
        String errorCode,
        @Schema(description = "Human-readable message.")
        String message,
        @Schema(description = "Conversation identifier, when available.")
        String conversationId,
        @Schema(description = "Correlation or request identifier, when available.")
        String requestId
) {
}
