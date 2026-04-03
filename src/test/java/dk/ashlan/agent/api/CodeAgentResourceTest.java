package dk.ashlan.agent.api;

import dk.ashlan.agent.code.CodeAgentOrchestrator;
import dk.ashlan.agent.code.CodeGenerationTool;
import dk.ashlan.agent.code.CommandResult;
import dk.ashlan.agent.code.FileReadTool;
import dk.ashlan.agent.code.FileWriteTool;
import dk.ashlan.agent.code.TestExecutionTool;
import dk.ashlan.agent.code.WorkspaceService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeAgentResourceTest {
    @Test
    void runReturnsGeneratedWorkspaceOutputAndTestStatus() throws Exception {
        Path workspaceRoot = Files.createTempDirectory("code-agent-test");
        WorkspaceService workspaceService = new WorkspaceService(workspaceRoot.toString());
        CodeAgentOrchestrator orchestrator = new CodeAgentOrchestrator(
                workspaceService,
                new FileReadTool(workspaceService),
                new FileWriteTool(workspaceService),
                new CodeGenerationTool(),
                new TestExecutionTool()
        );
        CodeAgentResource resource = new CodeAgentResource(orchestrator);

        Map<String, Object> response = resource.run(Map.of("message", "Generate a response file"));
        CommandResult testResult = orchestrator.runTests();

        assertEquals(0, testResult.exitCode());
        assertTrue(response.get("response").toString().contains("Workspace:"));
        assertTrue(response.get("testOutput").toString().contains("placeholder"));
    }
}
