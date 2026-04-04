package dk.ashlan.agent.document;

import java.nio.file.Path;
import java.util.List;

public record DocumentReadResult(
        String status,
        Path resolvedPath,
        String fileType,
        String contentType,
        String extractedText,
        String extractionNote,
        List<String> traceEvents,
        boolean success,
        boolean wasTruncated,
        int originalLength,
        int extractedLength
) {
}
