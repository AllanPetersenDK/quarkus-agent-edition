package dk.ashlan.agent.tools.filesystem;

import dk.ashlan.agent.tools.AbstractTool;
import dk.ashlan.agent.tools.JsonToolResult;
import dk.ashlan.agent.tools.ToolDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class ListFilesTool extends AbstractTool {
    private final FilesystemToolService filesystemToolService;

    @Inject
    public ListFilesTool(FilesystemToolService filesystemToolService) {
        this.filesystemToolService = filesystemToolService;
    }

    @Override
    public String name() {
        return "list_files";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "List files and folders inside the workspace root. Inputs: path, optional recursive, optional maxEntries.");
    }

    @Override
    public JsonToolResult execute(Map<String, Object> arguments) {
        return filesystemToolService.listFiles(stringArg(arguments, "path"), booleanArg(arguments, "recursive"), intArg(arguments, "maxEntries", 50));
    }

    @Override
    protected String executeSafely(Map<String, Object> arguments) {
        return execute(arguments).output();
    }

    private String stringArg(Map<String, Object> arguments, String key) {
        Object value = arguments == null ? null : arguments.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private boolean booleanArg(Map<String, Object> arguments, String key) {
        Object value = arguments == null ? null : arguments.get(key);
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private int intArg(Map<String, Object> arguments, String key, int defaultValue) {
        Object value = arguments == null ? null : arguments.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
