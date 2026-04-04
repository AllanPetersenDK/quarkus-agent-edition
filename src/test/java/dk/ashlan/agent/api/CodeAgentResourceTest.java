package dk.ashlan.agent.api;

import dk.ashlan.agent.code.CodeAgentOrchestrator;
import dk.ashlan.agent.code.CodeGenerationTool;
import dk.ashlan.agent.code.CodeWorkspaceRegistry;
import dk.ashlan.agent.code.FileReadTool;
import dk.ashlan.agent.code.FileWriteTool;
import dk.ashlan.agent.code.TestExecutionTool;
import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.api.dto.CodeAgentRunRequest;
import dk.ashlan.agent.memory.InMemorySessionTraceStore;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeAgentResourceTest {
    @Test
    void runReturnsGeneratedWorkspaceOutputAndTestStatus() throws Exception {
        Path workspaceRoot = Files.createTempDirectory("code-agent-test");
        WorkspaceService workspaceService = new WorkspaceService(workspaceRoot.toString());
        CodeAgentOrchestrator orchestrator = new CodeAgentOrchestrator(
                new CodeWorkspaceRegistry(workspaceRoot.resolve("sessions").toString(), new InMemorySessionTraceStore()),
                new FileReadTool(workspaceService),
                new FileWriteTool(workspaceService),
                new CodeGenerationTool(),
                new TestExecutionTool(workspaceService)
        );
        CodeAgentResource resource = new CodeAgentResource(orchestrator);

        var response = resource.run(new CodeAgentRunRequest("chapter8-demo", "Generate a response file"));

        assertTrue(response.runId().startsWith("chapter8-demo#"));
        assertEquals(0, response.testResult().exitCode());
        assertTrue(response.response().contains("Chapter 8 workspace response"));
        assertTrue(response.response().contains("Request: Generate a response file"));
        assertTrue(response.testResult().output().contains("Validation passed"));
        assertTrue(response.generatedTools().stream().anyMatch(tool -> "workspace-summary".equals(tool.name())));
        assertTrue(response.generatedTools().stream().anyMatch(tool -> tool.sourceArtifacts().contains("generated/response.txt")));
        assertTrue(response.generatedTools().stream().anyMatch(tool -> tool.invocationCount() == 0));
        assertTrue(response.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-run-start:" + response.runId())));
        assertTrue(response.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-workspace-created:")));
        assertTrue(response.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-file-written:generated/response.txt")));
        assertTrue(response.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-file-written:generated/skills/workspace-summary.md")));
        assertTrue(response.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-file-written:generated/tests/result.txt")));
        assertTrue(response.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-file-read:generated/response.txt")));
        assertTrue(response.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-validation-passed:" + response.runId())));
        assertTrue(response.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-test-executed:" + response.runId())));
        assertTrue(response.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-run-complete:" + response.runId())));
    }
}
