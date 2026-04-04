package dk.ashlan.agent.api.dto;

import dk.ashlan.agent.code.CodeAgentRunResult;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

public record CodeAgentRunResponse(
        @Schema(description = "Session identifier used for the Chapter 8 workspace.")
        String sessionId,
        @Schema(description = "Workspace identifier resolved from the session id.")
        String workspaceId,
        @Schema(description = "Stable run identifier for the current Chapter 8 execution.")
        String runId,
        @Schema(description = "Absolute workspace root path.")
        String workspaceRoot,
        @Schema(description = "Generated response text written into the workspace.")
        String response,
        @Schema(description = "Relative path to the generated code artifact.")
        String generatedArtifactPath,
        @Schema(description = "Relative path to the generated skill card.")
        String skillPath,
        @Schema(description = "Relative path to the generated test report.")
        String testReportPath,
        @Schema(description = "Number of files currently present in the workspace.")
        long fileCount,
        @Schema(description = "Exit code and output from the workspace validation seam.")
        CodeAgentRunResponse.TestResultResponse testResult,
        @Schema(description = "Session-scoped generated tools.")
        List<GeneratedWorkspaceToolResponse> generatedTools,
        @Schema(description = "Stable Chapter 8 trace markers captured during the run.")
        List<String> traceMarkers
) {
    public static CodeAgentRunResponse from(CodeAgentRunResult result) {
        return new CodeAgentRunResponse(
                result.sessionId(),
                result.workspaceId(),
                result.runId(),
                result.workspaceRoot(),
                result.response(),
                result.generatedArtifactPath(),
                result.skillPath(),
                result.testReportPath(),
                result.fileCount(),
                TestResultResponse.from(result.testResult()),
                result.generatedTools().stream().map(GeneratedWorkspaceToolResponse::from).toList(),
                result.traceMarkers()
        );
    }

    public record TestResultResponse(
            @Schema(description = "Exit code from the workspace validation run.")
            int exitCode,
            @Schema(description = "Human-readable validation output.")
            String output,
            @Schema(description = "Human-readable validation error output, if any.")
            String error
    ) {
        static TestResultResponse from(dk.ashlan.agent.code.CommandResult result) {
            return new TestResultResponse(result.exitCode(), result.output(), result.error());
        }
    }
}
