package dk.ashlan.agent.eval.gaia;

import java.util.List;
import java.util.Map;

public record GaiaExample(
        String taskId,
        String question,
        String level,
        String fileName,
        String filePath,
        String finalAnswer,
        List<String> goldenAnswers,
        Map<String, Object> rawMetadata,
        GaiaAttachment attachment
) {
    public GaiaExample withAttachment(GaiaAttachment attachment) {
        return new GaiaExample(taskId, question, level, fileName, filePath, finalAnswer, goldenAnswers, rawMetadata, attachment);
    }

    public List<String> expectedAnswers() {
        if (goldenAnswers != null && !goldenAnswers.isEmpty()) {
            return List.copyOf(goldenAnswers);
        }
        if (finalAnswer == null || finalAnswer.isBlank()) {
            return List.of();
        }
        return List.of(finalAnswer);
    }
}
