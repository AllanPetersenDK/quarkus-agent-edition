package dk.ashlan.agent.chapters.chapter08;

import dk.ashlan.agent.api.CodeAgentResource;
import dk.ashlan.agent.api.RuntimeInspectionResource;
import dk.ashlan.agent.api.dto.CodeWorkspaceFilesResponse;
import dk.ashlan.agent.api.dto.CodeWorkspaceInspectionResponse;
import dk.ashlan.agent.api.dto.CodeAgentRunRequest;
import dk.ashlan.agent.api.dto.GeneratedWorkspaceToolInvokeRequest;
import dk.ashlan.agent.api.dto.GeneratedWorkspaceToolsResponse;
import dk.ashlan.agent.api.dto.GeneratedWorkspaceToolInvokeResponse;
import dk.ashlan.agent.code.CodeAgentOrchestrator;
import dk.ashlan.agent.code.CodeGenerationTool;
import dk.ashlan.agent.code.CodeWorkspaceRegistry;
import dk.ashlan.agent.code.FileReadTool;
import dk.ashlan.agent.code.FileWriteTool;
import dk.ashlan.agent.code.TestExecutionTool;
import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.health.AgentReadinessHealthCheck;
import dk.ashlan.agent.health.RuntimeLivenessHealthCheck;
import dk.ashlan.agent.memory.InMemorySessionTraceStore;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryAwareAgentOrchestrator;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.eval.RuntimeRunHistoryStore;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter08RuntimeTest {
    @Test
    void workspaceSessionIsSafeInspectableAndGeneratesSessionScopedTools() throws Exception {
        Path tempDir = Files.createTempDirectory("chapter8-runtime-test");
        InMemorySessionTraceStore traceStore = new InMemorySessionTraceStore();
        CodeWorkspaceRegistry registry = new CodeWorkspaceRegistry(tempDir.resolve("workspaces").toString(), traceStore);
        WorkspaceService legacyWorkspace = new WorkspaceService(tempDir.resolve("legacy").toString());
        CodeAgentOrchestrator orchestrator = new CodeAgentOrchestrator(
                registry,
                new FileReadTool(legacyWorkspace),
                new FileWriteTool(legacyWorkspace),
                new CodeGenerationTool(),
                new TestExecutionTool(legacyWorkspace)
        );
        CodeAgentResource codeAgentResource = new CodeAgentResource(orchestrator);

        SessionManager sessionManager = new SessionManager();
        MemoryService memoryService = new MemoryService(sessionManager, new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        RuntimeInspectionResource runtimeResource = new RuntimeInspectionResource(
                new AgentReadinessHealthCheck(new dk.ashlan.agent.core.AgentOrchestrator(null, null, null, null, 1, "") {
                }, new ToolRegistry(java.util.List.of())),
                new RuntimeLivenessHealthCheck(),
                sessionManager,
                memoryService,
                traceStore,
                new MemoryAwareAgentOrchestrator(
                        new dk.ashlan.agent.core.AgentOrchestrator(null, null, null, null, 1, "") {
                        },
                        memoryService
                ),
                registry,
                new RuntimeRunHistoryStore()
        );

        CodeWorkspaceInspectionResponse emptyWorkspace = runtimeResource.workspace("chapter8-demo");
        assertEquals("chapter8-demo", emptyWorkspace.sessionId());
        assertTrue(emptyWorkspace.fileCount() >= 0);
        assertTrue(emptyWorkspace.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-workspace-created:")));

        CodeAgentRunRequest request = new CodeAgentRunRequest("chapter8-demo", "Generate a workspace helper");
        var firstRun = codeAgentResource.run(request);
        assertTrue(firstRun.runId().startsWith("chapter8-demo#"));
        assertTrue(firstRun.response().contains("Chapter 8 workspace response"));
        assertTrue(firstRun.testResult().output().contains("Validation passed"));
        assertTrue(firstRun.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-run-start:" + firstRun.runId())));
        assertTrue(firstRun.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-tool-generated:" + firstRun.runId())));
        assertTrue(firstRun.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-file-written:generated/response.txt")));
        assertTrue(firstRun.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-file-written:generated/skills/workspace-summary.md")));
        assertTrue(firstRun.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-file-written:generated/tests/result.txt")));
        assertTrue(firstRun.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-file-read:generated/response.txt")));
        assertTrue(firstRun.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-test-executed:" + firstRun.runId())));
        assertTrue(firstRun.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-run-complete:" + firstRun.runId())));

        CodeAgentRunRequest secondRequest = new CodeAgentRunRequest("chapter8-demo", "Generate a second workspace helper");
        var secondRun = codeAgentResource.run(secondRequest);
        assertTrue(secondRun.runId().startsWith("chapter8-demo#"));
        assertTrue(!firstRun.runId().equals(secondRun.runId()));
        assertTrue(secondRun.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-run-start:" + firstRun.runId())));
        assertTrue(secondRun.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-run-start:" + secondRun.runId())));
        assertTrue(secondRun.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-run-complete:" + secondRun.runId())));
        assertTrue(secondRun.response().contains("Chapter 8 workspace response"));

        CodeWorkspaceInspectionResponse workspace = runtimeResource.workspace("chapter8-demo");
        assertEquals("chapter8-demo", workspace.sessionId());
        assertTrue(workspace.fileCount() >= 3);
        assertTrue(workspace.generatedToolCount() >= 1);
        assertEquals(secondRun.runId(), workspace.lastRunId());
        assertEquals("Generate a second workspace helper", workspace.lastRequest());

        CodeWorkspaceFilesResponse files = runtimeResource.workspaceFiles("chapter8-demo");
        assertTrue(files.files().contains("generated/response.txt"));
        assertTrue(files.files().contains("generated/skills/workspace-summary.md"));
        assertTrue(files.files().contains("generated/tests/result.txt"));

        GeneratedWorkspaceToolsResponse tools = runtimeResource.generatedTools("chapter8-demo");
        assertEquals(1, tools.tools().size());
        assertEquals("workspace-summary", tools.tools().get(0).name());
        assertEquals(3, tools.tools().get(0).sourceArtifacts().size());
        assertEquals(0, tools.tools().get(0).invocationCount());
        assertTrue(tools.tools().get(0).lastInvokedAt() == null);

        GeneratedWorkspaceToolInvokeResponse invokeResponse = runtimeResource.generatedToolInvoke(
                "chapter8-demo",
                new GeneratedWorkspaceToolInvokeRequest("workspace-summary", Map.of("maxEntries", 10, "recursive", true))
        );
        assertTrue(invokeResponse.success());
        assertTrue(invokeResponse.output().contains("fileCount="));
        assertTrue(invokeResponse.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-tool-invoked:")));

        GeneratedWorkspaceToolsResponse toolsAfterInvoke = runtimeResource.generatedTools("chapter8-demo");
        assertEquals(1, toolsAfterInvoke.tools().size());
        assertEquals(1, toolsAfterInvoke.tools().get(0).invocationCount());
        assertTrue(toolsAfterInvoke.tools().get(0).lastInvokedAt() != null);
        assertTrue(toolsAfterInvoke.tools().get(0).sourceArtifacts().contains("generated/tests/result.txt"));

        var traceResponse = runtimeResource.trace("chapter8-demo");
        assertEquals("chapter8-demo", traceResponse.sessionId());
        assertTrue(traceResponse.steps().size() >= 4);
        assertTrue(traceResponse.steps().stream()
                .flatMap(step -> step.traceEntries().stream())
                .anyMatch(entry -> entry.message().startsWith("chapter8-run-start:" + firstRun.runId())));
        assertTrue(traceResponse.steps().stream()
                .flatMap(step -> step.traceEntries().stream())
                .anyMatch(entry -> entry.message().startsWith("chapter8-run-start:" + secondRun.runId())));
        assertTrue(traceResponse.steps().stream()
                .flatMap(step -> step.traceEntries().stream())
                .anyMatch(entry -> entry.message().startsWith("chapter8-file-written:generated/tests/result.txt")));
        assertTrue(traceResponse.steps().stream()
                .flatMap(step -> step.traceEntries().stream())
                .anyMatch(entry -> entry.message().startsWith("chapter8-run-complete:" + secondRun.runId())));
        assertTrue(traceResponse.finalAnswer().contains("Chapter 8 workspace response"));
        assertTrue(traceResponse.finalAnswer().contains("Generate a second workspace helper"));

        CodeWorkspaceInspectionResponse controlWorkspace = runtimeResource.workspace("chapter8-control");
        assertTrue(controlWorkspace.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-workspace-created:")));
        assertFalse(controlWorkspace.traceMarkers().stream().anyMatch(marker -> marker.startsWith("chapter8-tool-invoked:")));
    }
}
