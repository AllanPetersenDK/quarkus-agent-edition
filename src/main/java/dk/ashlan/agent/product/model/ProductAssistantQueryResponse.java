package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

public record ProductAssistantQueryResponse(
        @Schema(description = "Conversation identifier used to scope session and memory continuity.")
        String conversationId,
        @Schema(description = "Number of messages currently stored for the conversation.")
        int conversationMessageCount,
        @Schema(description = "Original user query.")
        String query,
        @Schema(description = "Answer assembled from the product assistant pipeline.")
        String answer,
        @Schema(description = "Top knowledge sources used by the assistant.")
        List<ProductSourceResponse> sources,
        @Schema(description = "Relevant memory hints recovered for the conversation.")
        List<String> memoryHints,
        @Schema(description = "Compact planning summary used to shape the answer.")
        ProductPlanResponse plan,
        @Schema(description = "Reflection result showing whether the answer passed the lightweight quality check.")
        ProductReflectionResponse reflection,
        @Schema(description = "Small product-lane signals that make the run easy to inspect without exposing chapter internals.")
        List<String> signals
) {
}
