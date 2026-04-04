package dk.ashlan.agent.eval.gaia;

import java.util.List;
import java.util.Map;

public record GaiaCaseResult(
        String runId,
        String taskId,
        String prompt,
        String level,
        List<String> expectedAnswers,
        String predictedAnswer,
        GaiaScoreResult score,
        boolean correct,
        Integer iterations,
        String stopReason,
        List<String> trace,
        List<String> attachmentEvents,
        GaiaAttachment attachment,
        Map<String, Object> sourceMetadata
) {
}
