package dk.ashlan.agent.chapters.chapter10;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter10SmokeTest {
    @Test
    void chapter10DemosWork() {
        var metrics = new MetricsDemo().run();
        assertTrue(new EvaluationRunDemo().run().stream().anyMatch(result -> result.passed()));
        assertTrue(new TraceDemo().run().events().contains("passed:true"));
        assertTrue(metrics.passed() >= 1);
        assertTrue(metrics.durationMillis() >= 0);
    }
}
