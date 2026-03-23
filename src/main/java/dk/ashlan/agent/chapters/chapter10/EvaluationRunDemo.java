package dk.ashlan.agent.chapters.chapter10;

import dk.ashlan.agent.eval.EvalResult;
import dk.ashlan.agent.eval.EvaluationRunner;
import dk.ashlan.agent.eval.AgentTraceService;

import java.util.List;

public class EvaluationRunDemo {
    public List<EvalResult> run() {
        AgentTraceService traceService = new AgentTraceService();
        EvaluationRunner runner = Chapter10Support.evaluationRunner(traceService);
        return runner.run(Chapter10Support.cases());
    }
}
