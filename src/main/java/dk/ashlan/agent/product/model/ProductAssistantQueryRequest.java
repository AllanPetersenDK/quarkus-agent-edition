package dk.ashlan.agent.product.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ProductAssistantQueryRequest(
        @Size(max = 128)
        @Pattern(regexp = "^[A-Za-z0-9._:-]+$", message = "conversationId may only contain letters, numbers, '.', '_', ':', and '-'")
        @Schema(description = "Optional conversation identifier used to keep the product assistant stateful across turns. If omitted, the server generates a stable product conversation id.", examples = {"product-conversation-1"})
        String conversationId,
        @NotBlank
        @Size(max = 4096)
        @Schema(description = "User question or knowledge request.", required = true, examples = {"Which text mentions PostgreSQL?"})
        String query,
        @Min(1)
        @Max(10)
        @Schema(description = "Maximum number of knowledge chunks to consider. Defaults to 3.", examples = {"3"})
        Integer topK
) {
    public ProductAssistantQueryRequest {
        conversationId = conversationId == null ? null : conversationId.trim();
        query = query == null ? "" : query.trim();
    }
}
