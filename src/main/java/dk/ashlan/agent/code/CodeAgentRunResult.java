package dk.ashlan.agent.code;

import java.util.List;

public record CodeAgentRunResult(
        String sessionId,
        String workspaceId,
        String runId,
        String workspaceRoot,
        String response,
        String generatedArtifactPath,
        String skillPath,
        String testReportPath,
        CommandResult testResult,
        long fileCount,
        List<GeneratedWorkspaceTool> generatedTools,
        List<String> traceMarkers
) {
}
