package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CodeGenerationTool {
    public String generate(String request) {
        String normalizedRequest = normalizeRequest(request);
        String headline = summarizeRequest(normalizedRequest);
        String greeting = normalizedRequest.toLowerCase().contains("hello")
                ? "Hello from the Chapter 8 workspace."
                : "The Chapter 8 workspace is ready for the requested code-agent task.";
        return """
                Chapter 8 workspace response
                Request: %s

                Summary: %s
                %s

                Workspace guidance:
                - keep generated files inside the workspace root
                - inspect generated/skills/workspace-summary.md for the session-scoped skill card
                - use generated/tests/result.txt to review validation output

                Next step: inspect the workspace files or invoke the generated workspace-summary tool.
                """.formatted(normalizedRequest, headline, greeting);
    }

    public String generateSkillCard(String request) {
        String normalizedRequest = normalizeRequest(request);
        String headline = summarizeRequest(normalizedRequest);
        return """
                # workspace-summary

                ## Purpose
                Summarize the current session-scoped Chapter 8 workspace.

                Request:
                %s

                ## Current focus
                %s

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
                """.formatted(normalizedRequest, headline);
    }

    private static String normalizeRequest(String request) {
        return request == null ? "" : request.trim().replaceAll("\\s+", " ");
    }

    private static String summarizeRequest(String request) {
        if (request == null || request.isBlank()) {
            return "No request supplied.";
        }
        String headline = request;
        if (headline.length() > 80) {
            headline = headline.substring(0, 80).trim() + "…";
        }
        return headline;
    }
}
