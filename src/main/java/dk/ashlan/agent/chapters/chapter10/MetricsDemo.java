package dk.ashlan.agent.chapters.chapter10;

import dk.ashlan.agent.eval.AgentTraceService;
import dk.ashlan.agent.eval.RunMetrics;

import java.util.concurrent.TimeUnit;

public class MetricsDemo {
    public RunMetrics run() {
        AgentTraceService traceService = new AgentTraceService();
        var runner = Chapter10Support.evaluationRunner(traceService);
        long started = System.nanoTime();
        var results = runner.run(Chapter10Support.cases());
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        return Chapter10Support.metrics(runner, results, durationMillis);
    }
}
