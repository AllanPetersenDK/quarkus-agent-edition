package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CodeAgentOrchestrator {
    private final CodeWorkspaceRegistry workspaceRegistry;
    private final FileReadTool fileReadTool;
    private final FileWriteTool fileWriteTool;
    private final CodeGenerationTool codeGenerationTool;
    private final TestExecutionTool testExecutionTool;

    public CodeAgentOrchestrator(
            CodeWorkspaceRegistry workspaceRegistry,
            FileReadTool fileReadTool,
            FileWriteTool fileWriteTool,
            CodeGenerationTool codeGenerationTool,
            TestExecutionTool testExecutionTool
    ) {
        this.workspaceRegistry = workspaceRegistry;
        this.fileReadTool = fileReadTool;
        this.fileWriteTool = fileWriteTool;
        this.codeGenerationTool = codeGenerationTool;
        this.testExecutionTool = testExecutionTool;
    }

    public String run(String request) {
        return run("chapter8-demo", request).response();
    }

    public CodeAgentRunResult run(String sessionId, String request) {
        CodeWorkspaceSession session = workspaceRegistry.session(sessionId);
        session.rememberRequest(request);
        String generated = codeGenerationTool.generate(request);
        session.recordTrace("chapter8-code-generated:generated/response.txt");
        fileWriteTool.write(session.workspaceService(), "generated/response.txt", generated);
        String skillCard = codeGenerationTool.generateSkillCard(request);
        fileWriteTool.write(session.workspaceService(), "generated/skills/workspace-summary.md", skillCard);
        session.registerWorkspaceSummaryTool(request, "generated/skills/workspace-summary.md");
        CommandResult testResult = testExecutionTool.runTests();
        session.recordTrace("chapter8-test-executed:placeholder");
        fileWriteTool.write(session.workspaceService(), "generated/tests/result.txt", testResult.output());
        String response = "Workspace: " + session.workspaceRoot() + "\n" + fileReadTool.read(session.workspaceService(), "generated/response.txt");
        return new CodeAgentRunResult(
                session.sessionId(),
                session.workspaceId(),
                session.workspaceRoot(),
                response,
                "generated/response.txt",
                "generated/skills/workspace-summary.md",
                "generated/tests/result.txt",
                testResult,
                session.fileCount(),
                session.generatedTools(),
                session.traceMarkers()
        );
    }

    public CommandResult runTests() {
        return testExecutionTool.runTests();
    }
}
