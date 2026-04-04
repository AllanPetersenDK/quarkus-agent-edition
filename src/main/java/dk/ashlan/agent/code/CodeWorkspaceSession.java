package dk.ashlan.agent.code;

import dk.ashlan.agent.tools.JsonToolResult;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CodeWorkspaceSession {
    private final String sessionId;
    private final String workspaceId;
    private final WorkspaceService workspaceService;
    private final Instant createdAt;
    private Instant updatedAt;
    private String lastRequest = "";
    private final List<String> traceMarkers = new ArrayList<>();
    private final Map<String, GeneratedWorkspaceTool> generatedTools = new LinkedHashMap<>();

    CodeWorkspaceSession(String sessionId, String workspaceId, Path workspaceRoot) {
        this.sessionId = sessionId;
        this.workspaceId = workspaceId;
        this.workspaceService = new WorkspaceService(workspaceRoot.toString());
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        recordTrace("chapter8-workspace-created:" + this.workspaceService.root());
    }

    public String sessionId() {
        return sessionId;
    }

    public String workspaceId() {
        return workspaceId;
    }

    public WorkspaceService workspaceService() {
        return workspaceService;
    }

    public synchronized Instant createdAt() {
        return createdAt;
    }

    public synchronized Instant updatedAt() {
        return updatedAt;
    }

    public synchronized String lastRequest() {
        return lastRequest;
    }

    public synchronized long fileCount() {
        return workspaceService.fileCount();
    }

    public synchronized List<String> files() {
        return workspaceService.listFiles();
    }

    public synchronized List<GeneratedWorkspaceTool> generatedTools() {
        return List.copyOf(generatedTools.values());
    }

    public synchronized List<String> traceMarkers() {
        return List.copyOf(traceMarkers);
    }

    public synchronized String workspaceRoot() {
        return workspaceService.root().toString();
    }

    public synchronized void rememberRequest(String request) {
        lastRequest = request == null ? "" : request;
        touch();
    }

    public synchronized void writeFile(String relativePath, String contents) {
        workspaceService.write(relativePath, contents);
        recordTrace("chapter8-file-written:" + relativePath);
    }

    public synchronized String readFile(String relativePath) {
        String contents = workspaceService.read(relativePath);
        recordTrace("chapter8-file-read:" + relativePath);
        return contents;
    }

    public synchronized GeneratedWorkspaceTool registerWorkspaceSummaryTool(String prompt, String skillPath) {
        GeneratedWorkspaceTool tool = new GeneratedWorkspaceTool(
                "workspace-summary",
                "Summarize the current workspace files and generated artifacts.",
                prompt == null ? "" : prompt,
                skillPath,
                Instant.now()
        );
        generatedTools.put(tool.name(), tool);
        recordTrace("chapter8-tool-generated:" + tool.name());
        return tool;
    }

    public synchronized JsonToolResult invokeGeneratedTool(String toolName, Map<String, Object> arguments) {
        GeneratedWorkspaceTool tool = generatedTools.get(toolName);
        if (tool == null) {
            return JsonToolResult.failure(toolName, "Unknown generated tool: " + toolName);
        }
        if (!"workspace-summary".equals(toolName)) {
            return JsonToolResult.failure(toolName, "Unsupported generated tool: " + toolName);
        }
        String path = stringArg(arguments, "path");
        boolean recursive = booleanArg(arguments, "recursive", true);
        int maxEntries = intArg(arguments, "maxEntries", 20);
        List<String> files = workspaceService.listFiles(path, recursive, maxEntries);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("workspaceId", workspaceId);
        data.put("workspaceRoot", workspaceService.root().toString());
        data.put("path", path == null ? "" : path);
        data.put("recursive", recursive);
        data.put("maxEntries", maxEntries);
        data.put("fileCount", workspaceService.fileCount());
        data.put("files", files);
        String output = "status=ok\nsessionId=" + sessionId + "\nworkspaceRoot=" + workspaceService.root()
                + "\nfileCount=" + workspaceService.fileCount() + "\nfiles:\n" + formatFiles(files);
        recordTrace("chapter8-tool-invoked:" + toolName);
        return new JsonToolResult(toolName, true, output, data);
    }

    public synchronized void recordTrace(String marker) {
        traceMarkers.add(marker);
        touch();
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    private static String formatFiles(List<String> files) {
        if (files.isEmpty()) {
            return "- (none)";
        }
        StringBuilder builder = new StringBuilder();
        for (String file : files) {
            builder.append("- ").append(file).append('\n');
        }
        return builder.toString().trim();
    }

    private static String stringArg(Map<String, Object> arguments, String key) {
        Object value = arguments == null ? null : arguments.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean booleanArg(Map<String, Object> arguments, String key, boolean defaultValue) {
        Object value = arguments == null ? null : arguments.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).toLowerCase(Locale.ROOT);
        return switch (text) {
            case "true", "1", "yes", "y" -> true;
            case "false", "0", "no", "n" -> false;
            default -> defaultValue;
        };
    }

    private static int intArg(Map<String, Object> arguments, String key, int defaultValue) {
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
