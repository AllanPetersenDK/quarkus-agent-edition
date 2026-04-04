package dk.ashlan.agent.rag;

import java.util.List;

public record RagDirectoryIngestFileResult(
        String path,
        String resolvedPath,
        String sourceId,
        String status,
        String documentStatus,
        String fileType,
        int chunkCount,
        List<String> traceEvents,
        String error
) {
}
