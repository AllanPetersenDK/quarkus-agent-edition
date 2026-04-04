package dk.ashlan.agent.eval.gaia;

public record GaiaScoreResult(
        boolean passed,
        double score,
        String reason,
        String expectedNormalized,
        String predictedNormalized,
        String matchedExpected
) {
}
