package dk.ashlan.agent.chapters.chapter10;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter10FlowTest {
    @Test
    void evaluationRunnerCapturesTraceAndMetrics() {
        var traceService = new dk.ashlan.agent.eval.AgentTraceService();
        var runner = Chapter10Support.evaluationRunner(traceService);
        long startedAt = System.nanoTime();
        var results = runner.run(Chapter10Support.cases());
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertEquals(2, results.size());
        assertTrue(traceService.get("calc").events().contains("passed:true"));
        assertEquals(2, runner.metrics(results, durationMillis).total());
    }
}
