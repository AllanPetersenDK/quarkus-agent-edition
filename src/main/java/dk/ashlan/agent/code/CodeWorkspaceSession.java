package dk.ashlan.agent.code;

import dk.ashlan.agent.core.AgentStepResult;
import dk.ashlan.agent.core.AgentTraceEntry;
import dk.ashlan.agent.memory.SessionTraceStore;
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
    private final SessionTraceStore traceStore;
    private final Instant createdAt;
    private Instant updatedAt;
    private String lastRequest = "";
    private String lastRunId = "";
    private final List<String> traceMarkers = new ArrayList<>();
    private final Map<String, GeneratedWorkspaceTool> generatedTools = new LinkedHashMap<>();
    private int traceStepNumber = 0;
    private int runSequence = 0;

    CodeWorkspaceSession(String sessionId, String workspaceId, Path workspaceRoot, SessionTraceStore traceStore) {
        this.sessionId = sessionId;
        this.workspaceId = workspaceId;
        this.workspaceService = new WorkspaceService(workspaceRoot.toString());
        this.traceStore = traceStore;
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

    public synchronized String lastRunId() {
        return lastRunId;
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

    public synchronized String beginRun(String request) {
        String runId = nextRunId();
        lastRequest = request == null ? "" : request;
        lastRunId = runId;
        touch();
        recordTrace("chapter8-run-start:" + runId);
        return runId;
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

    public synchronized GeneratedWorkspaceTool registerWorkspaceSummaryTool(String prompt, String skillPath, List<String> sourceArtifacts, String runId) {
        String effectiveRunId = runId == null || runId.isBlank() ? "run-unknown" : runId;
        GeneratedWorkspaceTool tool = new GeneratedWorkspaceTool(
                "workspace-summary",
                "Summarize the current workspace files and generated artifacts.",
                prompt == null ? "" : prompt,
                skillPath,
                sourceArtifacts == null ? List.of() : List.copyOf(sourceArtifacts),
                Instant.now(),
                null,
                0
        );
        generatedTools.put(tool.name(), tool);
        recordTrace("chapter8-tool-generated:" + effectiveRunId + ":" + tool.name());
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
        GeneratedWorkspaceTool updatedTool = tool.recordInvocation();
        generatedTools.put(toolName, updatedTool);
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
        data.put("invocationCount", updatedTool.invocationCount());
        data.put("lastInvokedAt", updatedTool.lastInvokedAt() == null ? null : updatedTool.lastInvokedAt().toString());
        data.put("sourceArtifacts", updatedTool.sourceArtifacts());
        data.put("files", files);
        String output = "status=ok\nsessionId=" + sessionId + "\nworkspaceRoot=" + workspaceService.root()
                + "\nfileCount=" + workspaceService.fileCount() + "\nfiles:\n" + formatFiles(files);
        String effectiveRunId = lastRunId == null || lastRunId.isBlank() ? "run-unknown" : lastRunId;
        recordTrace("chapter8-tool-invoked:" + effectiveRunId + ":" + toolName + ":" + updatedTool.invocationCount());
        return new JsonToolResult(toolName, true, output, data);
    }

    public synchronized void recordTrace(String marker) {
        if (marker == null || marker.isBlank()) {
            return;
        }
        appendTrace(marker, false, null);
    }

    public synchronized void recordFinalTrace(String marker, String finalAnswer) {
        if (marker == null || marker.isBlank()) {
            return;
        }
        appendTrace(marker, true, finalAnswer);
    }

    private void appendTrace(String marker, boolean finalStep, String finalAnswer) {
        traceMarkers.add(marker);
        if (traceStore != null) {
            traceStore.append(new AgentStepResult(
                    sessionId,
                    ++traceStepNumber,
                    null,
                    List.of(),
                    List.of(),
                    finalAnswer,
                    finalStep,
                    List.of(new AgentTraceEntry(traceKind(marker), marker))
            ));
        }
        touch();
    }

    private static String traceKind(String marker) {
        int index = marker.indexOf(':');
        return index > 0 ? marker.substring(0, index) : marker;
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

    private String nextRunId() {
        runSequence++;
        return String.format("%s#%04d", sessionId, runSequence);
    }
}
