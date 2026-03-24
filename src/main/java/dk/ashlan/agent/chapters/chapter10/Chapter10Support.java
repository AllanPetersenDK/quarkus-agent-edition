package dk.ashlan.agent.chapters.chapter10;

import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.AgentRunner;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.eval.AgentTrace;
import dk.ashlan.agent.eval.AgentTraceService;
import dk.ashlan.agent.eval.EvalCase;
import dk.ashlan.agent.eval.EvalResult;
import dk.ashlan.agent.eval.EvaluationRunner;
import dk.ashlan.agent.eval.RunMetrics;

import java.util.List;

final class Chapter10Support {
    private Chapter10Support() {
    }

    static EvaluationRunner evaluationRunner(AgentTraceService traceService) {
        AgentRunner runner = message -> {
            if (message.contains("25 * 4")) {
                return new AgentRunResult("The calculator tool returns 100", StopReason.FINAL_ANSWER, 1, List.of("answered:" + message));
            }
            return new AgentRunResult("No relevant knowledge found.", StopReason.FINAL_ANSWER, 1, List.of("answered:" + message));
        };
        return new EvaluationRunner(runner, traceService);
    }

    static List<EvalCase> cases() {
        return List.of(
                new EvalCase("calc", "What is 25 * 4?", "100"),
                new EvalCase("miss", "Tell me about penguins", "No relevant knowledge")
        );
    }

    static RunMetrics metrics(EvaluationRunner runner, List<EvalResult> results, long durationMillis) {
        return runner.metrics(results, durationMillis);
    }

    static AgentTrace trace(AgentTraceService traceService, String caseId) {
        return traceService.get(caseId);
    }
}
