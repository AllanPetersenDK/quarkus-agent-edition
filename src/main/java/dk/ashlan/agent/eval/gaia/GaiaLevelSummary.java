package dk.ashlan.agent.eval.gaia;

public record GaiaLevelSummary(
        int total,
        int passed,
        int failed,
        double accuracy,
        Double averageIterations
) {
}
