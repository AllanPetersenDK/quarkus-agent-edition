package dk.ashlan.agent.eval.gaia;

import java.util.List;

public record GaiaValidationCaseResult(
        String taskId,
        String question,
        String expectedAnswer,
        String predictedAnswer,
        boolean correct,
        Integer iterations,
        String stopReason,
        List<String> trace
) {
}
