package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;

@ApplicationScoped
public class FileReadTool {
    private final WorkspaceService workspaceService;

    public FileReadTool(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    public String read(String relativePath) {
        return workspaceService.read(relativePath);
    }

    public String read(WorkspaceService workspaceService, String relativePath) {
        return workspaceService.read(relativePath);
    }

    public String read(Path workspaceRoot, String relativePath) {
        return new WorkspaceService(workspaceRoot.toString()).read(relativePath);
    }
}
