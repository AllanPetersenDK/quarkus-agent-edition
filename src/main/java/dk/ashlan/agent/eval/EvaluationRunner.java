package dk.ashlan.agent.eval;

import dk.ashlan.agent.core.AgentRunner;
import dk.ashlan.agent.core.AgentRunResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EvaluationRunner {
    private final AgentRunner agentRunner;
    private final AgentTraceService traceService;

    public EvaluationRunner(AgentRunner agentRunner, AgentTraceService traceService) {
        this.agentRunner = agentRunner;
        this.traceService = traceService;
    }

    public List<EvalResult> run(List<EvalCase> cases) {
        List<EvalResult> results = new ArrayList<>();
        for (EvalCase evalCase : cases) {
            AgentRunResult output = agentRunner.run(evalCase.prompt());
            boolean passed = output.finalAnswer() != null && output.finalAnswer().contains(evalCase.expectedSubstring());
            List<String> events = new ArrayList<>(output.trace());
            events.add("passed:" + passed);
            traceService.record(evalCase.id(), events);
            results.add(new EvalResult(evalCase.id(), passed, output.finalAnswer(), passed ? "Matched expected substring." : "Did not match expected substring."));
        }
        return results;
    }

    public RunMetrics metrics(List<EvalResult> results, long durationMillis) {
        int passed = (int) results.stream().filter(EvalResult::passed).count();
        return new RunMetrics(results.size(), passed, results.size() - passed, durationMillis);
    }
}
