package dk.ashlan.agent.tools.filesystem;

import dk.ashlan.agent.tools.AbstractTool;
import dk.ashlan.agent.tools.JsonToolResult;
import dk.ashlan.agent.tools.ToolDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class ReadFileTool extends AbstractTool {
    private final FilesystemToolService filesystemToolService;

    @Inject
    public ReadFileTool(FilesystemToolService filesystemToolService) {
        this.filesystemToolService = filesystemToolService;
    }

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Read a text-based file inside the workspace root. Inputs: path.");
    }

    @Override
    public JsonToolResult execute(Map<String, Object> arguments) {
        return filesystemToolService.readFile(stringArg(arguments, "path"));
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
