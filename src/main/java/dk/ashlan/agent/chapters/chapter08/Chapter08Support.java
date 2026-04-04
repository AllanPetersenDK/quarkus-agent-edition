package dk.ashlan.agent.chapters.chapter08;

import dk.ashlan.agent.code.CodeAgentOrchestrator;
import dk.ashlan.agent.code.CodeGenerationTool;
import dk.ashlan.agent.code.CodeWorkspaceRegistry;
import dk.ashlan.agent.code.FileReadTool;
import dk.ashlan.agent.code.FileWriteTool;
import dk.ashlan.agent.code.TestExecutionTool;
import dk.ashlan.agent.code.WorkspaceService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class Chapter08Support {
    private Chapter08Support() {
    }

    static WorkspaceService workspaceService() {
        return new WorkspaceService("target/chapter-08-workspace");
    }

    static CodeWorkspaceRegistry workspaceRegistry() {
        return new CodeWorkspaceRegistry("target/chapter-08-workspaces");
    }

    static CodeAgentOrchestrator orchestrator() {
        WorkspaceService workspaceService = workspaceService();
        return new CodeAgentOrchestrator(
                workspaceRegistry(),
                new FileReadTool(workspaceService),
                new FileWriteTool(workspaceService),
                new CodeGenerationTool(),
                new TestExecutionTool(workspaceService)
        );
    }

    static String writeAndRead(String path, String contents) {
        WorkspaceService workspaceService = workspaceService();
        workspaceService.write(path, contents);
        return workspaceService.read(path);
    }

    static boolean pathTraversalRejected() {
        return rejectsTraversal(workspaceService());
    }

    private static boolean rejectsTraversal(WorkspaceService workspaceService) {
        try {
            workspaceService.resolve("../secret.txt");
            return false;
        } catch (IllegalArgumentException exception) {
            return true;
        }
    }

    static Path ensureWorkspace() throws IOException {
        Path root = Path.of("target/chapter-08-workspace").toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root;
    }
}
