package dk.ashlan.agent.code;

import java.time.Instant;

public record GeneratedWorkspaceTool(
        String name,
        String description,
        String prompt,
        String skillPath,
        Instant createdAt
) {
}
