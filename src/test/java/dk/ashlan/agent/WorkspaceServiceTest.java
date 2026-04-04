package dk.ashlan.agent;

import dk.ashlan.agent.code.WorkspaceService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceServiceTest {
    @Test
    void rejectsPathTraversal() throws Exception {
        Path workspace = Files.createTempDirectory("workspace-test");
        WorkspaceService workspaceService = new WorkspaceService(workspace.toString());

        assertThrows(IllegalArgumentException.class, () -> workspaceService.resolve("../secret.txt"));
    }

    @Test
    void readsAndWritesInsideWorkspace() throws Exception {
        Path workspace = Files.createTempDirectory("workspace-test");
        WorkspaceService workspaceService = new WorkspaceService(workspace.toString());

        workspaceService.write("nested/file.txt", "hello");

        assertTrue(workspaceService.read("nested/file.txt").contains("hello"));
        assertTrue(workspaceService.listFiles().contains("nested/file.txt"));
        assertTrue(workspaceService.fileCount() >= 1);
    }
}
