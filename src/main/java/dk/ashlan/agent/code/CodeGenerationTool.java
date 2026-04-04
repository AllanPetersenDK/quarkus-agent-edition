package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CodeGenerationTool {
    public String generate(String request) {
        return "// Chapter 8 code generation placeholder\n// Request: " + request;
    }

    public String generateSkillCard(String request) {
        return """
                # workspace-summary
                Generated Chapter 8 skill card.

                Request:
                %s

                This skill is intentionally narrow and session-scoped.
                """.formatted(request);
    }
}
