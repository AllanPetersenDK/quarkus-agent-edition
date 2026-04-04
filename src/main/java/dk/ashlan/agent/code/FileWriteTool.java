package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;

@ApplicationScoped
public class FileWriteTool {
    private final WorkspaceService workspaceService;

    public FileWriteTool(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    public void write(String relativePath, String contents) {
        workspaceService.write(relativePath, contents);
    }

    public void write(WorkspaceService workspaceService, String relativePath, String contents) {
        workspaceService.write(relativePath, contents);
    }

    public void write(Path workspaceRoot, String relativePath, String contents) {
        new WorkspaceService(workspaceRoot.toString()).write(relativePath, contents);
    }
}
