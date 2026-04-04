package dk.ashlan.agent.code;

import java.time.Instant;
import java.util.List;

public record GeneratedWorkspaceTool(
        String name,
        String description,
        String prompt,
        String skillPath,
        List<String> sourceArtifacts,
        Instant createdAt,
        Instant lastInvokedAt,
        int invocationCount
) {
    public GeneratedWorkspaceTool recordInvocation() {
        return new GeneratedWorkspaceTool(
                name,
                description,
                prompt,
                skillPath,
                sourceArtifacts,
                createdAt,
                Instant.now(),
                invocationCount + 1
        );
    }
}
