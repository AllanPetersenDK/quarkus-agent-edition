package dk.ashlan.agent.eval.gaia;

public record GaiaValidationCase(
        String taskId,
        String question,
        String finalAnswer,
        String level,
        String filePath
) {
}
