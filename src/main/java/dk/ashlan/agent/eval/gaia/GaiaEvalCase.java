package dk.ashlan.agent.eval.gaia;

import java.util.List;
import java.util.Map;

public record GaiaEvalCase(
        String taskId,
        String prompt,
        List<String> expectedAnswers,
        String level,
        String attachmentPath,
        GaiaAttachment attachment,
        Map<String, Object> sourceMetadata
) {
    public String expectedAnswer() {
        if (expectedAnswers == null || expectedAnswers.isEmpty()) {
            return "";
        }
        return expectedAnswers.getFirst();
    }
}
