package dk.ashlan.agent.chapters.chapter10;

import dk.ashlan.agent.eval.AgentTraceService;
import dk.ashlan.agent.eval.RunMetrics;

public class MetricsDemo {
    public RunMetrics run() {
        AgentTraceService traceService = new AgentTraceService();
        var runner = Chapter10Support.evaluationRunner(traceService);
        return Chapter10Support.metrics(runner, runner.run(Chapter10Support.cases()));
    }
}
