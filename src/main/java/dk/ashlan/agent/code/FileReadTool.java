package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FileReadTool {
    private final WorkspaceService workspaceService;

    public FileReadTool(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    public String read(String relativePath) {
        return workspaceService.read(relativePath);
    }
}
