package dk.ashlan.agent.tools.filesystem;

import dk.ashlan.agent.tools.AbstractTool;
import dk.ashlan.agent.tools.JsonToolResult;
import dk.ashlan.agent.tools.ToolDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class ReadDocumentFileTool extends AbstractTool {
    private final FilesystemToolService filesystemToolService;

    @Inject
    public ReadDocumentFileTool(FilesystemToolService filesystemToolService) {
        this.filesystemToolService = filesystemToolService;
    }

    @Override
    public String name() {
        return "read_document_file";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Read a document or media file inside the workspace root. PDFs are text-extracted; supported audio files are transcribed. Inputs: path.");
    }

    @Override
    public JsonToolResult execute(Map<String, Object> arguments) {
        return filesystemToolService.readDocumentFile(stringArg(arguments, "path"));
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
