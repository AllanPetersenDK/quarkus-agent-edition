package dk.ashlan.agent;

import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.AgentRunner;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.eval.AgentTraceService;
import dk.ashlan.agent.eval.EvalCase;
import dk.ashlan.agent.eval.EvalResult;
import dk.ashlan.agent.eval.EvaluationRunner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationRunnerTest {
    @Test
    void evaluationRunnerRunsCasesAndRecordsTrace() {
        AgentRunner runner = message -> new AgentRunResult("answer for " + message, StopReason.FINAL_ANSWER, 1, List.of("iteration:1"));
        AgentTraceService traceService = new AgentTraceService();
        EvaluationRunner evaluationRunner = new EvaluationRunner(runner, traceService);

        List<EvalResult> results = evaluationRunner.run(List.of(new EvalCase("case-1", "hello", "hello")));

        assertEquals(1, results.size());
        assertTrue(results.get(0).passed());
        assertTrue(traceService.get("case-1").events().contains("passed:true"));
    }
}
