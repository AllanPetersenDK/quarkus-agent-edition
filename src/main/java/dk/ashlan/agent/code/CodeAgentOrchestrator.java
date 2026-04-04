package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import dk.ashlan.agent.eval.RuntimeRunRecorder;

import java.time.Instant;

@ApplicationScoped
public class CodeAgentOrchestrator {
    private final CodeWorkspaceRegistry workspaceRegistry;
    private final FileReadTool fileReadTool;
    private final FileWriteTool fileWriteTool;
    private final CodeGenerationTool codeGenerationTool;
    private final TestExecutionTool testExecutionTool;
    private final RuntimeRunRecorder runRecorder;

    @Inject
    public CodeAgentOrchestrator(
            CodeWorkspaceRegistry workspaceRegistry,
            FileReadTool fileReadTool,
            FileWriteTool fileWriteTool,
            CodeGenerationTool codeGenerationTool,
            TestExecutionTool testExecutionTool
    ) {
        this(workspaceRegistry, fileReadTool, fileWriteTool, codeGenerationTool, testExecutionTool, null);
    }

    public CodeAgentOrchestrator(
            CodeWorkspaceRegistry workspaceRegistry,
            FileReadTool fileReadTool,
            FileWriteTool fileWriteTool,
            CodeGenerationTool codeGenerationTool,
            TestExecutionTool testExecutionTool,
            RuntimeRunRecorder runRecorder
    ) {
        this.workspaceRegistry = workspaceRegistry;
        this.fileReadTool = fileReadTool;
        this.fileWriteTool = fileWriteTool;
        this.codeGenerationTool = codeGenerationTool;
        this.testExecutionTool = testExecutionTool;
        this.runRecorder = runRecorder;
    }

    public String run(String request) {
        return run("chapter8-demo", request).response();
    }

    public CodeAgentRunResult run(String sessionId, String request) {
        Instant startedAt = Instant.now();
        CodeWorkspaceSession session = workspaceRegistry.session(sessionId);
        String runId = session.beginRun(request);
        String generated = codeGenerationTool.generate(request);
        session.recordTrace("chapter8-code-generated:" + runId + ":generated/response.txt");
        session.writeFile("generated/response.txt", generated);
        String skillCard = codeGenerationTool.generateSkillCard(request);
        session.writeFile("generated/skills/workspace-summary.md", skillCard);
        session.registerWorkspaceSummaryTool(
                request,
                "generated/skills/workspace-summary.md",
                java.util.List.of("generated/response.txt", "generated/skills/workspace-summary.md", "generated/tests/result.txt"),
                runId
        );
        CommandResult testResult = testExecutionTool.runTests(
                session.workspaceService(),
                request,
                "generated/response.txt",
                "generated/skills/workspace-summary.md",
                "generated/tests/result.txt",
                session.generatedTools()
        );
        session.recordTrace("chapter8-file-written:generated/tests/result.txt");
        session.recordTrace(testResult.exitCode() == 0
                ? "chapter8-validation-passed:" + runId + ":generated/tests/result.txt"
                : "chapter8-validation-failed:" + runId + ":generated/tests/result.txt");
        session.recordTrace(testResult.exitCode() == 0
                ? "chapter8-test-executed:" + runId + ":success"
                : "chapter8-test-executed:" + runId + ":failure");
        String response = """
                Workspace: %s

                %s
                """.formatted(
                session.workspaceRoot(),
                session.readFile("generated/response.txt")
        );
        session.recordFinalTrace("chapter8-run-complete:" + runId + ":generated/response.txt", response);
        CodeAgentRunResult result = new CodeAgentRunResult(
                session.sessionId(),
                session.workspaceId(),
                runId,
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
        if (runRecorder != null) {
            runRecorder.recordCodeRun(runId, sessionId, request, result, startedAt, Instant.now());
        }
        return result;
    }

    public CommandResult runTests() {
        return testExecutionTool.runTests();
    }
}
