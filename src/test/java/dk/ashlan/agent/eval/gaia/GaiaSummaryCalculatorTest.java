package dk.ashlan.agent.eval.gaia;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GaiaSummaryCalculatorTest {
    @Test
    void summarizesTotalsAccuracyAndAverageIterations() {
        GaiaSummaryCalculator calculator = new GaiaSummaryCalculator();
        GaiaValidationSummary summary = calculator.summarize(List.of(
                new GaiaValidationCaseResult("1", "q1", "a1", "a1", true, 2, "FINAL_ANSWER", List.of("t1")),
                new GaiaValidationCaseResult("2", "q2", "a2", "b2", false, 4, "MAX_ITERATIONS", List.of("t2"))
        ));

        assertEquals(2, summary.total());
        assertEquals(1, summary.correct());
        assertEquals(0.5d, summary.accuracy());
        assertEquals(3.0d, summary.averageIterations());
    }

    @Test
    void emptySummaryHasZeroAccuracyAndNoAverageIterations() {
        GaiaSummaryCalculator calculator = new GaiaSummaryCalculator();

        GaiaValidationSummary summary = calculator.summarize(List.of());

        assertEquals(0, summary.total());
        assertEquals(0, summary.correct());
        assertEquals(0.0d, summary.accuracy());
        assertNull(summary.averageIterations());
    }
}
