package dk.ashlan.agent.tools.filesystem;

import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.tools.AbstractTool;
import dk.ashlan.agent.tools.ToolDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@ApplicationScoped
public class DeleteFileTool extends AbstractTool {
    private final WorkspaceService workspaceService;

    @Inject
    public DeleteFileTool(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @Override
    public String name() {
        return "delete-file";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                name(),
                "Delete a workspace file. Requires explicit confirmation before execution.",
                true,
                "Approve deleting the requested workspace file?"
        );
    }

    @Override
    protected String executeSafely(Map<String, Object> arguments) {
        try {
            String pathValue = arguments == null ? null : String.valueOf(arguments.get("path"));
            if (pathValue == null || pathValue.isBlank()) {
                throw new IllegalArgumentException("path is required");
            }
            Path resolved = workspaceService.resolve(pathValue);
            if (!Files.exists(resolved)) {
                throw new IllegalArgumentException("file does not exist: " + resolved);
            }
            if (Files.isDirectory(resolved)) {
                throw new IllegalArgumentException("path is a directory: " + resolved);
            }
            boolean deleted = Files.deleteIfExists(resolved);
            return "status=ok\ndeleted=" + deleted + "\npath=" + pathValue + "\nresolvedPath=" + resolved;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("delete failed: " + exception.getMessage(), exception);
        }
    }
}
