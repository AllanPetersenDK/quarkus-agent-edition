package dk.ashlan.agent.product.model;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ProductAssistantQueryRequest(
        @Schema(description = "Optional conversation identifier used to keep the product assistant stateful across turns.", examples = {"product-conversation-1"})
        String conversationId,
        @NotBlank
        @Schema(description = "User question or knowledge request.", required = true, examples = {"Which text mentions PostgreSQL?"})
        String query,
        @Schema(description = "Maximum number of knowledge chunks to consider. Defaults to 3.", examples = {"3"})
        Integer topK
) {
    public ProductAssistantQueryRequest {
        conversationId = conversationId == null ? null : conversationId.trim();
        query = query == null ? "" : query.trim();
    }
}
