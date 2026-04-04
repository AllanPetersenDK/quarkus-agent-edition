package dk.ashlan.agent.eval.gaia;

import java.util.List;

final class GaiaSummaryCalculator {
    GaiaValidationSummary summarize(List<GaiaValidationCaseResult> results) {
        int total = results.size();
        int correct = (int) results.stream().filter(GaiaValidationCaseResult::correct).count();
        double accuracy = total == 0 ? 0.0 : (double) correct / (double) total;
        List<Integer> iterations = results.stream()
                .map(GaiaValidationCaseResult::iterations)
                .filter(value -> value != null && value > 0)
                .toList();
        Double averageIterations = iterations.isEmpty()
                ? null
                : iterations.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        return new GaiaValidationSummary(total, correct, accuracy, averageIterations);
    }
}
