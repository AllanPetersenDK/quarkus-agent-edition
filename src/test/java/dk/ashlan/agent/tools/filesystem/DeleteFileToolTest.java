package dk.ashlan.agent.tools.filesystem;

import dk.ashlan.agent.code.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteFileToolTest {
    @TempDir
    Path tempDir;

    @Test
    void deleteFileToolDeletesWorkspaceFileAndRequiresConfirmation() throws Exception {
        WorkspaceService workspaceService = new WorkspaceService(tempDir.resolve("workspace").toString());
        DeleteFileTool tool = new DeleteFileTool(workspaceService);
        Path file = workspaceService.resolve("temp.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "hello");

        assertTrue(tool.definition().requiresConfirmation());
        assertTrue(tool.execute(Map.of("path", "temp.txt")).success());
        assertFalse(Files.exists(file));
    }
}
