package dk.ashlan.agent.rag;

import java.util.Map;

public record DocumentChunk(
        String id,
        String sourceId,
        int chunkIndex,
        String text,
        Map<String, String> metadata
) {
}
