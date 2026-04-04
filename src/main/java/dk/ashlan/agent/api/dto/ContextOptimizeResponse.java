package dk.ashlan.agent.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

public record ContextOptimizeResponse(
        @Schema(description = "Token estimate for the original request projection.")
        int originalTokenCount,
        @Schema(description = "Token estimate after optimization.")
        int projectedTokenCount,
        @Schema(description = "Optimizer strategy chosen for the projection.")
        String strategy,
        @Schema(description = "Whether the optimizer changed the request projection.")
        boolean changed,
        @Schema(description = "Original messages passed into the optimizer.")
        List<ContextOptimizeRequest.ContextOptimizeMessage> originalMessages,
        @Schema(description = "Projected messages returned by the optimizer.")
        List<ContextOptimizeRequest.ContextOptimizeMessage> projectedMessages
) {
}
