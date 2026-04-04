package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@ApplicationScoped
public class TestExecutionTool {
    private final WorkspaceService workspaceService;

    public TestExecutionTool(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    public CommandResult runTests() {
        return runTests(
                workspaceService,
                "",
                "generated/response.txt",
                "generated/skills/workspace-summary.md",
                "generated/tests/result.txt",
                List.of()
        );
    }

    public CommandResult runTests(
            WorkspaceService workspaceService,
            String request,
            String responsePath,
            String skillPath,
            String reportPath,
            List<GeneratedWorkspaceTool> generatedTools
    ) {
        WorkspaceService effectiveWorkspace = workspaceService == null ? this.workspaceService : workspaceService;
        String normalizedRequest = normalize(request);
        List<String> failures = new ArrayList<>();

        String response = readFile(effectiveWorkspace, responsePath, "response", failures);
        String skillCard = readFile(effectiveWorkspace, skillPath, "skill card", failures);

        if (response.isBlank()) {
            failures.add("response artifact is empty");
        }
        if (containsPlaceholder(response)) {
            failures.add("response artifact still looks like a placeholder");
        }
        if (!isRequestAware(response, normalizedRequest)) {
            failures.add("response artifact is not request-aware");
        }
        if (!response.contains("Chapter 8 workspace response")) {
            failures.add("response artifact is missing the Chapter 8 workspace header");
        }
        if (!skillCard.contains("# workspace-summary")) {
            failures.add("skill card is missing the workspace-summary title");
        }
        if (!skillCard.contains("## Purpose")) {
            failures.add("skill card is missing the Purpose section");
        }
        if (!skillCard.contains("## Workspace boundaries")) {
            failures.add("skill card is missing the workspace boundaries section");
        }
        if (!skillCard.contains("## Generated artifacts")) {
            failures.add("skill card is missing the generated artifacts section");
        }
        if (!skillCard.contains("generated/response.txt") || !skillCard.contains("generated/tests/result.txt")) {
            failures.add("skill card does not describe the generated artifacts");
        }
        GeneratedWorkspaceTool workspaceSummaryTool = workspaceSummaryTool(generatedTools);
        if (workspaceSummaryTool == null) {
            failures.add("generated tool registry is missing workspace-summary");
        } else {
            if (!Objects.equals(skillPath, workspaceSummaryTool.skillPath())) {
                failures.add("workspace-summary skill path does not match the skill card");
            }
            if (!workspaceSummaryTool.sourceArtifacts().contains(responsePath)
                    || !workspaceSummaryTool.sourceArtifacts().contains(skillPath)
                    || !workspaceSummaryTool.sourceArtifacts().contains(reportPath)) {
                failures.add("workspace-summary source artifacts do not describe the generated workspace files");
            }
            if (workspaceSummaryTool.invocationCount() != 0) {
                failures.add("workspace-summary invocation count should be zero before invocation");
            }
            if (workspaceSummaryTool.lastInvokedAt() != null) {
                failures.add("workspace-summary should not have a last invocation timestamp before invocation");
            }
        }

        String report = buildReport(normalizedRequest, responsePath, skillPath, generatedTools, response, skillCard, failures);
        effectiveWorkspace.write(reportPath, report);

        if (failures.isEmpty()) {
            return new CommandResult(
                    0,
                    "Validation passed: response, skill card, and generated tool registry are aligned.",
                    ""
            );
        }
        return new CommandResult(
                1,
                "Validation failed: " + String.join("; ", failures),
                String.join("; ", failures)
        );
    }

    private static String readFile(WorkspaceService workspaceService, String path, String label, List<String> failures) {
        try {
            return workspaceService.read(path);
        } catch (RuntimeException exception) {
            failures.add("missing " + label + " artifact at " + path);
            return "";
        }
    }

    private static boolean containsPlaceholder(String text) {
        return normalize(text).toLowerCase(Locale.ROOT).contains("placeholder");
    }

    private static boolean isRequestAware(String response, String request) {
        String normalizedResponse = normalize(response).toLowerCase(Locale.ROOT);
        if (request.isBlank()) {
            return normalizedResponse.contains("chapter 8 workspace response");
        }
        return normalizedResponse.contains(request.toLowerCase(Locale.ROOT));
    }

    private static GeneratedWorkspaceTool workspaceSummaryTool(List<GeneratedWorkspaceTool> generatedTools) {
        return workspaceSummaryTool(generatedTools, null);
    }

    private static GeneratedWorkspaceTool workspaceSummaryTool(List<GeneratedWorkspaceTool> generatedTools, String skillPath) {
        if (generatedTools == null || generatedTools.isEmpty()) {
            return null;
        }
        return generatedTools.stream().filter(Objects::nonNull).filter(tool ->
                "workspace-summary".equals(tool.name())
                        && (skillPath == null || Objects.equals(skillPath, tool.skillPath())))
                .findFirst()
                .orElse(null);
    }

    private static String buildReport(
            String request,
            String responsePath,
            String skillPath,
            List<GeneratedWorkspaceTool> generatedTools,
            String response,
            String skillCard,
            List<String> failures
    ) {
        int toolCount = generatedTools == null ? 0 : generatedTools.size();
        GeneratedWorkspaceTool workspaceSummaryTool = workspaceSummaryTool(generatedTools, skillPath);
        return """
                # Chapter 8 validation report

                ## Request
                %s

                ## Checked artifacts
                - %s
                - %s
                - generated/tests/result.txt

                ## Generated tools
                - tool count: %d
                - workspace-summary present: %s
                - workspace-summary invocation count: %s
                - workspace-summary last invocation: %s
                - workspace-summary source artifacts: %s

                ## Response excerpt
                %s

                ## Skill card excerpt
                %s

                ## Result
                %s
                """.formatted(
                request.isBlank() ? "(no request supplied)" : request,
                responsePath,
                skillPath,
                toolCount,
                workspaceSummaryTool == null ? "no" : "yes",
                workspaceSummaryTool == null ? "n/a" : workspaceSummaryTool.invocationCount(),
                workspaceSummaryTool == null ? "n/a" : workspaceSummaryTool.lastInvokedAt(),
                workspaceSummaryTool == null ? "n/a" : workspaceSummaryTool.sourceArtifacts(),
                excerpt(response),
                excerpt(skillCard),
                failures.isEmpty()
                        ? "PASS"
                        : "FAIL: " + String.join("; ", failures)
        );
    }

    private static String excerpt(String text) {
        String normalized = normalize(text);
        if (normalized.length() <= 240) {
            return normalized;
        }
        return normalized.substring(0, 240).trim() + "…";
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }
}
