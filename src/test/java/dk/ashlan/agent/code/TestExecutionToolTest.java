package dk.ashlan.agent.code;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestExecutionToolTest {
    @Test
    void validationPassesForRequestAwareArtifacts() throws Exception {
        Path workspaceRoot = Files.createTempDirectory("chapter8-validation-pass");
        WorkspaceService workspaceService = new WorkspaceService(workspaceRoot.toString());
        TestExecutionTool tool = new TestExecutionTool(workspaceService);

        workspaceService.write("generated/response.txt", """
                Chapter 8 workspace response
                Request: write a hello-world style response

                Summary: write a hello-world style response
                Hello from the Chapter 8 workspace.
                """);
        workspaceService.write("generated/skills/workspace-summary.md", """
                # workspace-summary

                ## Purpose
                Summarize the current session-scoped Chapter 8 workspace.

                Request:
                write a hello-world style response

                ## Current focus
                write a hello-world style response

                ## Workspace boundaries
                - All file access stays under the workspace root.
                - Generated tools are session-scoped.
                - Validation is deterministic and workspace-local.

                ## Generated artifacts
                - generated/response.txt
                - generated/skills/workspace-summary.md
                - generated/tests/result.txt

                ## Usage
                Invoke `workspace-summary` to inspect the current files and generated artifacts.
                """);

        CommandResult result = tool.runTests(
                workspaceService,
                "write a hello-world style response",
                "generated/response.txt",
                "generated/skills/workspace-summary.md",
                "generated/tests/result.txt",
                List.of(new GeneratedWorkspaceTool(
                        "workspace-summary",
                        "Summarize the current workspace files and generated artifacts.",
                        "write a hello-world style response",
                        "generated/skills/workspace-summary.md",
                        List.of("generated/response.txt", "generated/skills/workspace-summary.md", "generated/tests/result.txt"),
                        Instant.now(),
                        null,
                        0
                ))
        );

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("Validation passed"));
        String report = workspaceService.read("generated/tests/result.txt");
        assertTrue(report.contains("# Chapter 8 validation report"));
        assertTrue(report.contains("workspace-summary invocation count: 0"));
        assertTrue(report.contains("workspace-summary source artifacts"));
    }

    @Test
    void validationFailsForPlaceholderArtifacts() throws Exception {
        Path workspaceRoot = Files.createTempDirectory("chapter8-validation-fail");
        WorkspaceService workspaceService = new WorkspaceService(workspaceRoot.toString());
        TestExecutionTool tool = new TestExecutionTool(workspaceService);

        workspaceService.write("generated/response.txt", "// Chapter 8 code generation placeholder");
        workspaceService.write("generated/skills/workspace-summary.md", "# workspace-summary\nplaceholder");

        CommandResult result = tool.runTests(
                workspaceService,
                "write a hello-world style response",
                "generated/response.txt",
                "generated/skills/workspace-summary.md",
                "generated/tests/result.txt",
                List.of()
        );

        assertEquals(1, result.exitCode());
        assertTrue(result.error().contains("placeholder"));
        assertTrue(workspaceService.read("generated/tests/result.txt").contains("FAIL"));
    }
}
