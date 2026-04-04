package dk.ashlan.agent.rag;

import java.util.List;

public record RagDirectoryIngestResult(
        String path,
        String resolvedPath,
        boolean recursive,
        int maxFiles,
        int totalCandidates,
        int ingestedCount,
        int skippedCount,
        int failedCount,
        List<RagDirectoryIngestFileResult> results,
        String error
) {
}
