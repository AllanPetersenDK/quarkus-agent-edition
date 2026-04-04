package dk.ashlan.agent.eval.gaia;

public record GaiaValidationSummary(
        int total,
        int correct,
        double accuracy,
        Double averageIterations
) {
}
