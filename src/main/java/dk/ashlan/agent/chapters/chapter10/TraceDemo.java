package dk.ashlan.agent.chapters.chapter10;

import dk.ashlan.agent.eval.AgentTrace;
import dk.ashlan.agent.eval.AgentTraceService;

public class TraceDemo {
    public AgentTrace run() {
        AgentTraceService traceService = new AgentTraceService();
        Chapter10Support.evaluationRunner(traceService).run(Chapter10Support.cases());
        return Chapter10Support.trace(traceService, "calc");
    }
}
