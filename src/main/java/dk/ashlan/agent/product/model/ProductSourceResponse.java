package dk.ashlan.agent.product.model;

import dk.ashlan.agent.rag.DocumentChunk;
import dk.ashlan.agent.rag.RetrievalResult;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Map;

public record ProductSourceResponse(
        @Schema(description = "Knowledge source identifier.")
        String sourceId,
        @Schema(description = "Workspace-relative path when available.")
        String sourcePath,
        @Schema(description = "Zero-based chunk index within the source document.")
        int chunkIndex,
        @Schema(description = "Stable chunk identifier.")
        String chunkId,
        @Schema(description = "Short excerpt from the selected source chunk.")
        String excerpt,
        @Schema(description = "Similarity score returned by retrieval.")
        Double similarity
) {
    public static ProductSourceResponse from(RetrievalResult result) {
        DocumentChunk chunk = result.chunk();
        Map<String, String> metadata = chunk.metadata();
        return new ProductSourceResponse(
                chunk.sourceId(),
                metadata.getOrDefault("sourcePath", ""),
                chunk.chunkIndex(),
                metadata.getOrDefault("chunkId", ""),
                firstSentenceOrTrimmed(chunk.text()),
                result.similarity()
        );
    }

    private static String firstSentenceOrTrimmed(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.trim().replace('\n', ' ');
        String[] sentences = normalized.split("(?<=[.!?])\\s+");
        if (sentences.length == 0) {
            return normalized;
        }
        return sentences[0].trim();
    }
}
