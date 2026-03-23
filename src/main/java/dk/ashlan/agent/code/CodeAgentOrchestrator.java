package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CodeAgentOrchestrator {
    private final WorkspaceService workspaceService;
    private final FileReadTool fileReadTool;
    private final FileWriteTool fileWriteTool;
    private final CodeGenerationTool codeGenerationTool;
    private final TestExecutionTool testExecutionTool;

    public CodeAgentOrchestrator(
            WorkspaceService workspaceService,
            FileReadTool fileReadTool,
            FileWriteTool fileWriteTool,
            CodeGenerationTool codeGenerationTool,
            TestExecutionTool testExecutionTool
    ) {
        this.workspaceService = workspaceService;
        this.fileReadTool = fileReadTool;
        this.fileWriteTool = fileWriteTool;
        this.codeGenerationTool = codeGenerationTool;
        this.testExecutionTool = testExecutionTool;
    }

    public String run(String request) {
        String generated = codeGenerationTool.generate(request);
        fileWriteTool.write("generated/response.txt", generated);
        return "Workspace: " + workspaceService.root() + "\n" + fileReadTool.read("generated/response.txt");
    }

    public CommandResult runTests() {
        return testExecutionTool.runTests();
    }
}
