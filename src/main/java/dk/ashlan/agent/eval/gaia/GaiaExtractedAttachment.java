package dk.ashlan.agent.eval.gaia;

import java.util.List;

public record GaiaExtractedAttachment(
        GaiaAttachmentStatus status,
        String contentType,
        String fileType,
        String extractedText,
        String extractionNote,
        List<String> traceEvents,
        boolean wasTruncated,
        int originalLength,
        int extractedLength
) {
    public boolean hasText() {
        return status == GaiaAttachmentStatus.TEXT_EXTRACTED;
    }
}
