package dk.ashlan.agent.eval.gaia;

import java.util.List;

public record GaiaAttachment(
        String fileName,
        String filePath,
        String resolvedPath,
        GaiaAttachmentStatus status,
        String note,
        List<String> traceEvents
) {
    public boolean present() {
        return status == GaiaAttachmentStatus.PRESENT;
    }

    public boolean supported() {
        return status == GaiaAttachmentStatus.PRESENT;
    }
}
