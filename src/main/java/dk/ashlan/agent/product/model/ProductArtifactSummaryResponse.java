package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Compact product artifact summary for product lists, details, and run inspection.")
public record ProductArtifactSummaryResponse(
        @Schema(description = "Stable artifact identifier.")
        String artifactId,
        @Schema(description = "Product run identifier, when the artifact belongs to a run.")
        String runId,
        @Schema(description = "Conversation identifier, when the artifact belongs to a conversation.")
        String conversationId,
        @Schema(description = "Artifact type such as knowledge-source, run-summary, trace-summary, or answer-preview.")
        String type,
        @Schema(description = "Short human-readable title.")
        String title,
        @Schema(description = "Artifact content type.")
        String contentType,
        @Schema(description = "Optional size hint in bytes.")
        Long sizeBytes,
        @Schema(description = "Artifact creation timestamp.")
        Instant createdAt,
        @Schema(description = "Short product-friendly preview.")
        String preview,
        @Schema(description = "Workspace-relative or source-relative path, when available.")
        String path,
        @Schema(description = "Stable source identifier, when available.")
        String sourceId
) {
}
