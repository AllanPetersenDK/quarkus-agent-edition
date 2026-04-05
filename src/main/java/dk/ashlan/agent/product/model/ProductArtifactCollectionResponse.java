package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Product artifact collection for a run or conversation.")
public record ProductArtifactCollectionResponse(
        @Schema(description = "Product run identifier, when available.")
        String runId,
        @Schema(description = "Conversation identifier, when available.")
        String conversationId,
        @Schema(description = "Number of artifacts in the collection.")
        int artifactCount,
        @Schema(description = "Artifacts available for the requested run or conversation.")
        List<ProductArtifactSummaryResponse> artifacts
) {
    public ProductArtifactCollectionResponse {
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
