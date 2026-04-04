package dk.ashlan.agent.eval;

import dk.ashlan.agent.code.CodeAgentOrchestrator;
import dk.ashlan.agent.code.CodeAgentRunResult;
import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.multiagent.AgentTaskResult;
import dk.ashlan.agent.multiagent.CoordinatorAgent;
import dk.ashlan.agent.product.model.ProductAssistantQueryRequest;
import dk.ashlan.agent.product.model.ProductAssistantQueryResponse;
import dk.ashlan.agent.product.service.ProductAssistantService;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class Chapter10EvaluationService {
    private final AgentOrchestrator agentOrchestrator;
    private final ProductAssistantService productAssistantService;
    private final CodeAgentOrchestrator codeAgentOrchestrator;
    private final CoordinatorAgent coordinatorAgent;
    private final RuntimeRunRecorder runRecorder;
    private final RuntimeRunHistoryStore historyStore;

    public Chapter10EvaluationService(
            AgentOrchestrator agentOrchestrator,
            ProductAssistantService productAssistantService,
            CodeAgentOrchestrator codeAgentOrchestrator,
            CoordinatorAgent coordinatorAgent,
            RuntimeRunRecorder runRecorder,
            RuntimeRunHistoryStore historyStore
    ) {
        this.agentOrchestrator = agentOrchestrator;
        this.productAssistantService = productAssistantService;
        this.codeAgentOrchestrator = codeAgentOrchestrator;
        this.coordinatorAgent = coordinatorAgent;
        this.runRecorder = runRecorder;
        this.historyStore = historyStore;
    }

    public Chapter10EvalRunResult run(Chapter10EvalRunRequest request) {
        String runId = historyStore.nextRunId();
        Instant startedAt = Instant.now();
        List<Chapter10EvalCaseResult> results = new ArrayList<>();
        for (Chapter10EvalCase evalCase : request.cases()) {
            results.add(runCase(runId, evalCase));
        }
        Instant endedAt = Instant.now();
        int total = results.size();
        int passed = (int) results.stream().filter(Chapter10EvalCaseResult::passed).count();
        int failed = total - passed;
        double averageScore = total == 0 ? 0.0 : results.stream().mapToDouble(Chapter10EvalCaseResult::score).average().orElse(0.0);
        String summary = "Evaluation run " + (request.name() == null || request.name().isBlank() ? runId : request.name())
                + " completed with " + passed + " passed and " + failed + " failed cases.";
        List<String> qualitySignals = List.of(
                "cases:" + total,
                "passed:" + passed,
                "failed:" + failed,
                "score:" + averageScore
        );
        runRecorder.recordChapter10EvaluationRun(runId, request.name(), results, startedAt, endedAt);
        return new Chapter10EvalRunResult(runId, request.name(), startedAt, total, passed, failed, averageScore, summary, qualitySignals, results);
    }

    private Chapter10EvalCaseResult runCase(String evalRunId, Chapter10EvalCase evalCase) {
        String caseRunId = evalRunId + "-" + evalCase.caseId();
        String lane = normalizeLane(evalCase.targetLane());
        Instant startedAt = Instant.now();
        return switch (lane) {
            case "manual" -> evaluateManual(evalCase, caseRunId, startedAt);
            case "product" -> evaluateProduct(evalCase);
            case "code" -> evaluateCode(evalCase, caseRunId);
            case "multi-agent" -> evaluateMultiAgent(evalCase);
            default -> throw new IllegalArgumentException("Unsupported chapter-10 evaluation lane: " + evalCase.targetLane());
        };
    }

    private Chapter10EvalCaseResult evaluateManual(Chapter10EvalCase evalCase, String sessionId, Instant startedAt) {
        AgentRunResult result = agentOrchestrator.run(evalCase.input(), sessionId);
        RuntimeRunRecord observation = runRecorder.recordManualRun(sessionId, evalCase.input(), result, startedAt, Instant.now());
        return buildCaseResult(evalCase, observation.runId(), result.finalAnswer(), observation.selectedTraceEntries(), observation.qualitySignals(), score(evalCase, result.finalAnswer(), observation.qualitySignals()), passed(evalCase, result.finalAnswer(), observation.qualitySignals()), explanation(evalCase, result.finalAnswer(), observation.qualitySignals()), failureReason(evalCase, result.finalAnswer(), observation.qualitySignals()));
    }

    private Chapter10EvalCaseResult evaluateProduct(Chapter10EvalCase evalCase) {
        String conversationId = "chapter10-product-" + evalCase.caseId();
        ProductAssistantQueryResponse response = productAssistantService.query(new ProductAssistantQueryRequest(conversationId, evalCase.input(), evalCase.topK()));
        List<String> signals = response.signals() == null ? List.of() : response.signals();
        List<String> selectedTrace = new ArrayList<>();
        selectedTrace.add("chapter10-run-start:" + response.runId());
        selectedTrace.add("chapter10-lane:product");
        selectedTrace.addAll(signals.stream().limit(6).toList());
        selectedTrace.add("chapter10-run-complete:" + response.runId());
        return buildCaseResult(evalCase, response.runId(), response.answer(), selectedTrace, signals, score(evalCase, response.answer(), signals), passed(evalCase, response.answer(), signals), explanation(evalCase, response.answer(), signals), failureReason(evalCase, response.answer(), signals));
    }

    private Chapter10EvalCaseResult evaluateCode(Chapter10EvalCase evalCase, String sessionId) {
        CodeAgentRunResult response = codeAgentOrchestrator.run(sessionId, evalCase.input());
        List<String> signals = List.of(
                "workspaceRoot:" + response.workspaceRoot(),
                "files:" + response.fileCount(),
                "generatedTools:" + (response.generatedTools() == null ? 0 : response.generatedTools().size()),
                "validation:" + (response.testResult() != null && response.testResult().exitCode() == 0 ? "passed" : "failed")
        );
        List<String> selectedTrace = new ArrayList<>();
        selectedTrace.add("chapter10-run-start:" + response.runId());
        selectedTrace.add("chapter10-lane:code");
        if (response.traceMarkers() != null) {
            selectedTrace.addAll(response.traceMarkers().stream().limit(6).toList());
        }
        selectedTrace.add("chapter10-run-" + ((response.testResult() != null && response.testResult().exitCode() == 0) ? "complete" : "failed") + ":" + response.runId());
        return buildCaseResult(evalCase, response.runId(), response.response(), selectedTrace, signals, score(evalCase, response.response(), signals), passed(evalCase, response.response(), signals), explanation(evalCase, response.response(), signals), failureReason(evalCase, response.response(), signals));
    }

    private Chapter10EvalCaseResult evaluateMultiAgent(Chapter10EvalCase evalCase) {
        AgentTaskResult response = coordinatorAgent.run(evalCase.input());
        List<String> signals = List.of(
                "route:" + response.agentName(),
                "routeReason:" + response.routeReason(),
                "review:" + (response.approved() ? "approved" : "rejected")
        );
        List<String> selectedTrace = new ArrayList<>();
        selectedTrace.add("chapter10-run-start:" + response.runId());
        selectedTrace.add("chapter10-lane:multi-agent");
        if (response.traceEntries() != null) {
            selectedTrace.addAll(response.traceEntries().stream().limit(6).toList());
        }
        selectedTrace.add("chapter10-run-" + (response.approved() ? "complete" : "rejected") + ":" + response.runId());
        return buildCaseResult(evalCase, response.runId(), response.output(), selectedTrace, signals, score(evalCase, response.output(), signals), passed(evalCase, response.output(), signals), explanation(evalCase, response.output(), signals), failureReason(evalCase, response.output(), signals));
    }

    private Chapter10EvalCaseResult buildCaseResult(
            Chapter10EvalCase evalCase,
            String runId,
            String output,
            List<String> traceEntries,
            List<String> observedSignals,
            double score,
            boolean passed,
            String explanation,
            String failureReason
    ) {
        return new Chapter10EvalCaseResult(
                evalCase.caseId(),
                normalizeLane(evalCase.targetLane()),
                runId,
                evalCase.input(),
                output,
                passed,
                score,
                explanation,
                failureReason,
                observedSignals,
                traceEntries,
                traceEntries == null ? "" : String.join(" | ", traceEntries)
        );
    }

    private double score(Chapter10EvalCase evalCase, String output, List<String> observedSignals) {
        double base = 0.0;
        String expected = evalCase.expectedSubstring();
        if (expected != null && !expected.isBlank() && output != null && output.contains(expected)) {
            base += 0.7;
        }
        if (evalCase.expectedSignals() != null && !evalCase.expectedSignals().isEmpty()) {
            long matched = evalCase.expectedSignals().stream()
                    .filter(signal -> observedSignalsContain(output, signal) || observedSignalsContain(observedSignals, signal))
                    .count();
            base += 0.3 * ((double) matched / (double) evalCase.expectedSignals().size());
        } else if (base == 0.0) {
            base = output == null || output.isBlank() ? 0.0 : 0.5;
        }
        return Math.min(1.0, base);
    }

    private boolean passed(Chapter10EvalCase evalCase, String output, List<String> observedSignals) {
        double threshold = evalCase.minimumScore() == null ? 0.5 : evalCase.minimumScore();
        return score(evalCase, output, observedSignals) >= threshold && (evalCase.expectedSignals() == null || evalCase.expectedSignals().stream().allMatch(signal -> observedSignalsContain(output, signal) || observedSignalsContain(observedSignals, signal)));
    }

    private String explanation(Chapter10EvalCase evalCase, String output, List<String> observedSignals) {
        if (passed(evalCase, output, observedSignals)) {
            return "Matched the expected output and inspection signals for lane " + normalizeLane(evalCase.targetLane()) + ".";
        }
        String expected = evalCase.expectedSubstring();
        if (expected != null && !expected.isBlank() && (output == null || !output.contains(expected))) {
            return "Output did not contain the expected substring: " + expected;
        }
        if (evalCase.expectedSignals() != null && !evalCase.expectedSignals().isEmpty()) {
            return "Output did not expose all expected inspection signals for lane " + normalizeLane(evalCase.targetLane()) + ".";
        }
        return "Case did not meet the evaluation threshold.";
    }

    private String failureReason(Chapter10EvalCase evalCase, String output, List<String> observedSignals) {
        return passed(evalCase, output, observedSignals) ? null : explanation(evalCase, output, observedSignals);
    }

    private boolean observedSignalsContain(String haystack, String needle) {
        return haystack != null && needle != null && !needle.isBlank() && haystack.contains(needle);
    }

    private boolean observedSignalsContain(List<String> haystack, String needle) {
        return haystack != null && needle != null && !needle.isBlank() && haystack.stream().anyMatch(signal -> signal != null && signal.contains(needle));
    }

    private String normalizeLane(String lane) {
        return lane == null ? "" : lane.trim().toLowerCase();
    }
}
