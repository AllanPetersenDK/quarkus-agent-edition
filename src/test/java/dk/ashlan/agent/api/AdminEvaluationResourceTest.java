package dk.ashlan.agent.api;

import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.AgentRunner;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.eval.AgentTraceService;
import dk.ashlan.agent.eval.EvalCase;
import dk.ashlan.agent.eval.EvaluationRunner;
import dk.ashlan.agent.eval.RunMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminEvaluationResourceTest {
    @Test
    void runEvaluationsReturnsMetricsAndResultList() {
        AgentTraceService traceService = new AgentTraceService();
        AgentRunner agentRunner = message -> new AgentRunResult(
                "The answer is 100",
                StopReason.FINAL_ANSWER,
                2,
                List.of("tool:calculator:100", "answer:The answer is 100")
        );
        EvaluationRunner evaluationRunner = new EvaluationRunner(agentRunner, traceService);
        AdminEvaluationResource resource = new AdminEvaluationResource(evaluationRunner, traceService);

        Map<String, Object> response = resource.run(List.of(new EvalCase("calc", "What is 25 * 4?", "100")));
        RunMetrics metrics = (RunMetrics) response.get("metrics");
        @SuppressWarnings("unchecked")
        List<?> results = (List<?>) response.get("results");
        AdminEvaluationResource.EvaluationTraceResponse trace = resource.trace("calc");

        assertEquals(1, metrics.total());
        assertEquals(1, metrics.passed());
        assertEquals(0, metrics.failed());
        assertEquals(1, results.size());
        assertEquals("calc", trace.caseId());
        assertEquals(3, trace.events().size());
    }
}
