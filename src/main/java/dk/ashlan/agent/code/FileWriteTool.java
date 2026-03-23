package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FileWriteTool {
    private final WorkspaceService workspaceService;

    public FileWriteTool(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    public void write(String relativePath, String contents) {
        workspaceService.write(relativePath, contents);
    }
}
