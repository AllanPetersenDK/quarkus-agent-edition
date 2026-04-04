package dk.ashlan.agent.document;

import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentReadServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void rawPathTraversalIsReturnedAsStructuredSecurityViolation() throws Exception {
        WorkspaceService workspaceService = new WorkspaceService(tempDir.resolve("workspace").toString());
        DocumentReadService service = new DocumentReadService(workspaceService, new GaiaAttachmentExtractionService(), audioTranscriptionService());

        DocumentReadResult result = service.readDocumentFile("../outside.txt");

        assertEquals("SECURITY_VIOLATION", result.status());
        assertTrue(result.extractionNote().contains("Path traversal is not allowed"));
        assertTrue(result.traceEvents().contains("document:path-rejected"));
    }

    @Test
    void blankRawPathIsReturnedAsInvalidPath() throws Exception {
        WorkspaceService workspaceService = new WorkspaceService(tempDir.resolve("workspace").toString());
        DocumentReadService service = new DocumentReadService(workspaceService, new GaiaAttachmentExtractionService(), audioTranscriptionService());

        DocumentReadResult result = service.readDocumentFile(" ");

        assertEquals("INVALID_PATH", result.status());
        assertTrue(result.traceEvents().contains("document:invalid-path"));
    }

    private GaiaAudioTranscriptionService audioTranscriptionService() {
        return path -> "unused audio transcript";
    }
}
