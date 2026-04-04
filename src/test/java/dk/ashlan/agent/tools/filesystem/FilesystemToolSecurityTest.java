package dk.ashlan.agent.tools.filesystem;

import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import dk.ashlan.agent.tools.JsonToolResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilesystemToolSecurityTest {
    @TempDir
    Path tempDir;

    @Test
    void rejectsSymlinkBypassAndSharesWorkspaceRootWithCodeWorkspaceService() throws Exception {
        Assumptions.assumeFalse(System.getProperty("os.name", "").toLowerCase().contains("win"));

        Path workspaceRoot = tempDir.resolve("workspace");
        Path outsideRoot = tempDir.resolve("outside");
        Files.createDirectories(workspaceRoot);
        Files.createDirectories(outsideRoot);

        WorkspaceService workspaceService = new WorkspaceService(workspaceRoot.toString());
        FilesystemToolService service = new FilesystemToolService(workspaceService.root(), new GaiaAttachmentExtractionService(), audioTranscriptionService());

        assertEquals(workspaceService.root(), service.workspaceRoot());

        Files.writeString(workspaceRoot.resolve("safe.txt"), "inside workspace");
        Files.writeString(outsideRoot.resolve("secret.txt"), "outside workspace");

        Files.createSymbolicLink(workspaceRoot.resolve("linked-file.txt"), outsideRoot.resolve("secret.txt"));
        Files.createDirectories(outsideRoot.resolve("escape-target"));
        Files.createSymbolicLink(workspaceRoot.resolve("linked-dir"), outsideRoot.resolve("escape-target"));

        Path zip = workspaceRoot.resolve("archive.zip");
        writeZip(zip, "docs/note.txt", "zip contents");
        Files.createSymbolicLink(workspaceRoot.resolve("linked-extract"), outsideRoot.resolve("escape-target"));

        JsonToolResult inspectSafe = new InspectPathTool(service).execute(Map.of("path", "safe.txt"));
        assertTrue(inspectSafe.success());

        assertSymlinkRejected(new ReadFileTool(service).execute(Map.of("path", "linked-file.txt")));
        assertSymlinkRejected(new ReadDocumentFileTool(service).execute(Map.of("path", "linked-file.txt")));
        assertSymlinkRejected(new ReadMediaFileTool(service).execute(Map.of("path", "linked-file.txt")));
        assertSymlinkRejected(new InspectPathTool(service).execute(Map.of("path", "linked-file.txt")));
        assertSymlinkRejected(new ListFilesTool(service).execute(Map.of("path", "linked-dir", "recursive", true, "maxEntries", 10)));
        assertSymlinkRejected(new UnzipFileTool(service).execute(Map.of("zipPath", "archive.zip", "extractTo", "linked-extract")));

        assertTrue(new ReadFileTool(service).execute(Map.of("path", "safe.txt")).success());
    }

    private GaiaAudioTranscriptionService audioTranscriptionService() {
        return path -> "unused audio transcript";
    }

    private void assertSymlinkRejected(JsonToolResult result) {
        assertFalse(result.success());
        assertTrue(result.output().contains("Symlink access is not allowed"));
    }

    private void writeZip(Path zipPath, String entryName, String content) throws Exception {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            ZipEntry entry = new ZipEntry(entryName);
            zipOutputStream.putNextEntry(entry);
            zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
    }
}
