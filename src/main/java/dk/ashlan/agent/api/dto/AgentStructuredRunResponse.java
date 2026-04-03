package dk.ashlan.agent.api.dto;

import dk.ashlan.agent.core.StopReason;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Chapter-4 structured-output runtime response for the companion demo schema.")
public record AgentStructuredRunResponse(
        @Schema(description = "Session identifier used for the structured-output run.")
        String sessionId,
        @Schema(description = "Requested structured-output mode.")
        String mode,
        @Schema(description = "Validation outcome for the controlled chapter-4 demo schema.")
        StructuredOutputValidationStatus validationStatus,
        @Schema(description = "Structured result returned by the chapter-4 demo schema.", nullable = true)
        StructuredAnswerResponse structuredResult,
        @Schema(description = "The raw one-step agent result used to build the structured output response.")
        AgentStepResponse step,
        @Schema(description = "Optional stop reason for the outer step seam.", nullable = true)
        StopReason stopReason
) {
    public record StructuredAnswerResponse(
            @Schema(description = "Normalized structured answer content.")
            String answer
    ) {
    }

    public enum StructuredOutputValidationStatus {
        VALIDATED,
        INVALID_SCHEMA
    }
}
