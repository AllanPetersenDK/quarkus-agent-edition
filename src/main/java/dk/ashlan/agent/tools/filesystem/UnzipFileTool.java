package dk.ashlan.agent.tools.filesystem;

import dk.ashlan.agent.tools.AbstractTool;
import dk.ashlan.agent.tools.JsonToolResult;
import dk.ashlan.agent.tools.ToolDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class UnzipFileTool extends AbstractTool {
    private final FilesystemToolService filesystemToolService;

    @Inject
    public UnzipFileTool(FilesystemToolService filesystemToolService) {
        this.filesystemToolService = filesystemToolService;
    }

    @Override
    public String name() {
        return "unzip_file";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Unzip an archive inside the workspace root. Inputs: zipPath and optional extractTo.");
    }

    @Override
    public JsonToolResult execute(Map<String, Object> arguments) {
        return filesystemToolService.unzipFile(stringArg(arguments, "zipPath"), stringArg(arguments, "extractTo"));
    }

    @Override
    protected String executeSafely(Map<String, Object> arguments) {
        return execute(arguments).output();
    }

    private String stringArg(Map<String, Object> arguments, String key) {
        Object value = arguments == null ? null : arguments.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
