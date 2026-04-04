package dk.ashlan.agent.eval;

import dk.ashlan.agent.api.dto.AgentStepResponse;
import dk.ashlan.agent.code.CodeAgentRunResult;
import dk.ashlan.agent.eval.gaia.GaiaRunResult;
import dk.ashlan.agent.multiagent.AgentTaskResult;
import dk.ashlan.agent.product.model.ProductAssistantQueryResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class RuntimeRunRecorder {
    private final RuntimeRunHistoryStore historyStore;

    public RuntimeRunRecorder(RuntimeRunHistoryStore historyStore) {
        this.historyStore = historyStore;
    }

    public String nextRunId() {
        return historyStore.nextRunId();
    }

    public RuntimeRunRecord recordManualRun(String sessionId, String input, dk.ashlan.agent.core.AgentRunResult result, Instant startedAt, Instant endedAt) {
        String runId = historyStore.nextRunId();
        List<String> trace = result.trace() == null ? List.of() : result.trace();
        List<String> selectedTrace = selectedEntries(trace, 6);
        List<String> historyTrace = new ArrayList<>();
        historyTrace.add("chapter10-run-start:" + runId);
        historyTrace.add("chapter10-lane:manual");
        historyTrace.addAll(selectedTrace);
        String status = "COMPLETED";
        String outcomeCategory = statusFromStopReason(result.stopReason());
        String failureReason = null;
        String errorCategory = null;
        if (result.stopReason() == dk.ashlan.agent.core.StopReason.TOOL_ERROR) {
            status = "FAILED";
            outcomeCategory = "tool_error";
            failureReason = "tool_error";
            errorCategory = "tool_error";
        } else if (result.stopReason() == dk.ashlan.agent.core.StopReason.PENDING_CONFIRMATION) {
            outcomeCategory = "pending_confirmation";
        } else if (result.stopReason() == dk.ashlan.agent.core.StopReason.REFLECTION_REJECTED) {
            status = "REJECTED";
            outcomeCategory = "rejected";
            failureReason = "reflection_rejected";
            errorCategory = "rejected";
        }
        historyTrace.add("chapter10-run-" + status.toLowerCase() + ":" + runId);
        return historyStore.record(new RuntimeRunRecord(
                runId,
                "manual",
                "run",
                sessionId,
                null,
                null,
                input,
                compactInput(input),
                startedAt,
                endedAt,
                duration(startedAt, endedAt),
                status,
                outcomeCategory,
                result.finalAnswer(),
                compactTrace(historyTrace),
                historyTrace,
                toolUsageSummary(trace, result.pendingToolCalls() == null ? 0 : result.pendingToolCalls().size()),
                List.of(
                        "iterations:" + result.iterations(),
                        "stopReason:" + result.stopReason(),
                        "pendingToolCalls:" + (result.pendingToolCalls() == null ? 0 : result.pendingToolCalls().size())
                ),
                null,
                null,
                countMatches(trace, "retrieval"),
                countMatches(trace, "tool"),
                null,
                null,
                null,
                null,
                failureReason,
                errorCategory
        ));
    }

    public RuntimeRunRecord recordManualStep(String sessionId, String input, AgentStepResponse step, Instant startedAt, Instant endedAt) {
        String runId = historyStore.nextRunId();
        List<String> trace = step.traceEntries() == null ? List.of() : step.traceEntries().stream()
                .map(entry -> entry.kind() + ":" + entry.message())
                .toList();
        List<String> selectedTrace = selectedEntries(trace, 6);
        List<String> historyTrace = new ArrayList<>();
        historyTrace.add("chapter10-run-start:" + runId);
        historyTrace.add("chapter10-lane:manual");
        historyTrace.addAll(selectedTrace);
        historyTrace.add("chapter10-run-complete:" + runId);
        return historyStore.record(new RuntimeRunRecord(
                runId,
                "manual",
                "step",
                sessionId,
                null,
                null,
                input,
                compactInput(input),
                startedAt,
                endedAt,
                duration(startedAt, endedAt),
                "COMPLETED",
                step.isFinal() ? "final_answer" : "step",
                step.finalAnswer(),
                compactTrace(historyTrace),
                historyTrace,
                toolUsageSummary(trace, step.toolCalls() == null ? 0 : step.toolCalls().size()),
                List.of(
                        "stepNumber:" + step.stepNumber(),
                        "toolCalls:" + (step.toolCalls() == null ? 0 : step.toolCalls().size()),
                        "toolResults:" + (step.toolResults() == null ? 0 : step.toolResults().size()),
                        "isFinal:" + step.isFinal()
                ),
                null,
                null,
                null,
                step.toolCalls() == null ? 0 : step.toolCalls().size(),
                null,
                null,
                null,
                null,
                null
        ));
    }

    public RuntimeRunRecord recordManualStructured(String sessionId, String input, AgentStepResponse step, Instant startedAt, Instant endedAt) {
        String runId = historyStore.nextRunId();
        List<String> trace = step.traceEntries() == null ? List.of() : step.traceEntries().stream()
                .map(entry -> entry.kind() + ":" + entry.message())
                .toList();
        List<String> selectedTrace = selectedEntries(trace, 6);
        List<String> historyTrace = new ArrayList<>();
        historyTrace.add("chapter10-run-start:" + runId);
        historyTrace.add("chapter10-lane:manual");
        historyTrace.addAll(selectedTrace);
        historyTrace.add("chapter10-run-complete:" + runId);
        return historyStore.record(new RuntimeRunRecord(
                runId,
                "manual",
                "structured",
                sessionId,
                null,
                null,
                input,
                compactInput(input),
                startedAt,
                endedAt,
                duration(startedAt, endedAt),
                "COMPLETED",
                step.isFinal() ? "final_answer" : "structured",
                step.finalAnswer(),
                compactTrace(historyTrace),
                historyTrace,
                toolUsageSummary(trace, step.toolCalls() == null ? 0 : step.toolCalls().size()),
                List.of(
                        "stepNumber:" + step.stepNumber(),
                        "toolCalls:" + (step.toolCalls() == null ? 0 : step.toolCalls().size()),
                        "toolResults:" + (step.toolResults() == null ? 0 : step.toolResults().size()),
                        "isFinal:" + step.isFinal()
                ),
                null,
                null,
                null,
                step.toolCalls() == null ? 0 : step.toolCalls().size(),
                null,
                null,
                null,
                null,
                null
        ));
    }

    public RuntimeRunRecord recordProductRun(String runId, String conversationId, String query, ProductAssistantQueryResponse response, Instant startedAt, Instant endedAt) {
        List<String> signals = response.signals() == null ? List.of() : response.signals();
        List<String> selectedTrace = selectedEntries(signals, 6);
        List<String> historyTrace = new ArrayList<>();
        historyTrace.add("chapter10-run-start:" + runId);
        historyTrace.add("chapter10-lane:product");
        historyTrace.addAll(selectedTrace);
        int sourceCount = response.sources() == null ? 0 : response.sources().size();
        int planSteps = response.plan() == null ? 0 : response.plan().stepCount();
        boolean accepted = response.reflection() != null && response.reflection().accepted();
        String status = accepted ? "COMPLETED" : "REJECTED";
        String outcomeCategory = accepted ? (sourceCount > 0 ? "answered_with_sources" : "answered_without_sources") : "reflection_rejected";
        String failureReason = accepted ? null : (response.reflection() == null ? "reflection rejected the answer" : response.reflection().feedback());
        historyTrace.add("chapter10-run-" + (accepted ? "completed" : "rejected") + ":" + runId);
        return historyStore.record(new RuntimeRunRecord(
                runId,
                "product",
                "query",
                null,
                conversationId,
                null,
                query,
                compactInput(query),
                startedAt,
                endedAt,
                duration(startedAt, endedAt),
                status,
                outcomeCategory,
                response.answer(),
                compactTrace(historyTrace),
                historyTrace,
                "retrievals=" + sourceCount + ", memoryHints=" + (response.memoryHints() == null ? 0 : response.memoryHints().size()) + ", planSteps=" + planSteps,
                signals,
                sourceCount,
                sourceCount,
                sourceCount,
                null,
                planSteps,
                accepted,
                accepted ? 1.0 : 0.0,
                accepted ? null : "reflection_rejected",
                failureReason,
                accepted ? null : "reflection_rejected"
        ));
    }

    public RuntimeRunRecord recordCodeRun(String runId, String sessionId, String request, CodeAgentRunResult result, Instant startedAt, Instant endedAt) {
        List<String> trace = result.traceMarkers() == null ? List.of() : result.traceMarkers();
        List<String> selectedTrace = selectedEntries(trace, 8);
        List<String> historyTrace = new ArrayList<>();
        historyTrace.add("chapter10-run-start:" + runId);
        historyTrace.add("chapter10-lane:code");
        historyTrace.addAll(selectedTrace);
        boolean passed = result.testResult() != null && result.testResult().exitCode() == 0;
        String status = passed ? "COMPLETED" : "FAILED";
        String outcomeCategory = passed ? "validation_passed" : "validation_failed";
        String failureReason = passed ? null : (result.testResult() == null ? "validation did not return a result" : nullToEmpty(result.testResult().error()).isBlank() ? nullToEmpty(result.testResult().output()) : result.testResult().error());
        historyTrace.add("chapter10-run-" + (passed ? "complete" : "failed") + ":" + runId);
        List<String> signals = new ArrayList<>();
        signals.add("workspaceRoot:" + result.workspaceRoot());
        signals.add("files:" + result.fileCount());
        signals.add("generatedTools:" + (result.generatedTools() == null ? 0 : result.generatedTools().size()));
        signals.add("validation:" + (passed ? "passed" : "failed"));
        return historyStore.record(new RuntimeRunRecord(
                runId,
                "code",
                "run",
                sessionId,
                null,
                null,
                request,
                compactInput(request),
                startedAt,
                endedAt,
                duration(startedAt, endedAt),
                status,
                outcomeCategory,
                result.response(),
                compactTrace(historyTrace),
                historyTrace,
                "generatedTools=" + (result.generatedTools() == null ? 0 : result.generatedTools().size()) + ", validation=" + (passed ? "pass" : "fail"),
                signals,
                null,
                null,
                null,
                result.generatedTools() == null ? 0 : result.generatedTools().size(),
                null,
                passed,
                passed ? 1.0 : 0.0,
                null,
                failureReason,
                passed ? null : "validation_failed"
        ));
    }

    public RuntimeRunRecord recordMultiAgentRun(String runId, String objective, AgentTaskResult result, Instant startedAt, Instant endedAt) {
        List<String> trace = result.traceEntries() == null ? List.of() : result.traceEntries();
        List<String> selectedTrace = selectedEntries(trace, 8);
        List<String> historyTrace = new ArrayList<>();
        historyTrace.add("chapter10-run-start:" + runId);
        historyTrace.add("chapter10-lane:multi-agent");
        historyTrace.addAll(selectedTrace);
        boolean approved = result.approved();
        String status = approved ? "COMPLETED" : "REJECTED";
        String outcomeCategory = approved ? "approved" : "rejected";
        List<String> signals = List.of(
                "route:" + result.agentName(),
                "routeReason:" + result.routeReason(),
                "review:" + (approved ? "approved" : "rejected")
        );
        historyTrace.add("chapter10-run-" + (approved ? "complete" : "rejected") + ":" + runId);
        return historyStore.record(new RuntimeRunRecord(
                runId,
                "multi-agent",
                "run",
                null,
                null,
                null,
                objective,
                compactInput(objective),
                startedAt,
                endedAt,
                duration(startedAt, endedAt),
                status,
                outcomeCategory,
                result.output(),
                compactTrace(historyTrace),
                historyTrace,
                "specialist=" + result.agentName() + ", reviewer=" + (approved ? "approved" : "rejected"),
                signals,
                null,
                null,
                null,
                null,
                null,
                approved,
                approved ? 1.0 : 0.0,
                approved ? null : result.review(),
                approved ? null : result.review(),
                approved ? null : "review_rejected"
        ));
    }

    public RuntimeRunRecord recordLegacyEvaluationRun(String runId, String objective, List<dk.ashlan.agent.eval.EvalResult> results, RunMetrics metrics, Instant startedAt, Instant endedAt) {
        List<String> selectedTrace = new ArrayList<>();
        selectedTrace.add("chapter10-run-start:" + runId);
        selectedTrace.add("chapter10-lane:evaluation");
        boolean passed = metrics.failed() == 0;
        selectedTrace.add(passed ? "chapter10-run-complete:" + runId : "chapter10-run-failed:" + runId);
        List<String> signals = List.of(
                "cases:" + metrics.total(),
                "passed:" + metrics.passed(),
                "failed:" + metrics.failed(),
                "durationMs:" + metrics.durationMillis()
        );
        return historyStore.record(new RuntimeRunRecord(
                runId,
                "evaluation",
                "legacy-run",
                null,
                null,
                null,
                objective,
                compactInput(objective),
                startedAt,
                endedAt,
                duration(startedAt, endedAt),
                "COMPLETED",
                passed ? "passed" : "failed",
                "Evaluation run completed with " + metrics.passed() + " passed and " + metrics.failed() + " failed cases.",
                compactTrace(selectedTrace),
                selectedTrace,
                "cases=" + metrics.total() + ", passed=" + metrics.passed() + ", failed=" + metrics.failed(),
                signals,
                null,
                null,
                null,
                null,
                null,
                passed,
                passed ? 1.0 : 0.0,
                null,
                passed ? null : "One or more evaluation cases failed.",
                passed ? null : "evaluation_failed"
        ));
    }

    public RuntimeRunRecord recordChapter10EvaluationRun(String runId, String objective, List<Chapter10EvalCaseResult> caseResults, Instant startedAt, Instant endedAt) {
        int total = caseResults.size();
        int passed = (int) caseResults.stream().filter(Chapter10EvalCaseResult::passed).count();
        int failed = total - passed;
        double score = total == 0 ? 0.0 : caseResults.stream().mapToDouble(result -> result.score()).average().orElse(0.0);
        boolean ok = failed == 0;
        List<String> selectedTrace = new ArrayList<>();
        selectedTrace.add("chapter10-run-start:" + runId);
        selectedTrace.add("chapter10-lane:evaluation");
        selectedTrace.addAll(caseResults.stream().flatMap(result -> result.selectedTraceEntries().stream()).limit(5).toList());
        selectedTrace.add(ok ? "chapter10-run-complete:" + runId : "chapter10-run-failed:" + runId);
        List<String> signals = List.of(
                "cases:" + total,
                "passed:" + passed,
                "failed:" + failed,
                "score:" + score
        );
        return historyStore.record(new RuntimeRunRecord(
                runId,
                "evaluation",
                "case-run",
                null,
                null,
                null,
                objective,
                compactInput(objective),
                startedAt,
                endedAt,
                duration(startedAt, endedAt),
                ok ? "COMPLETED" : "REJECTED",
                ok ? "passed" : "failed",
                ok ? "Evaluation cases passed." : "Evaluation cases failed.",
                compactTrace(selectedTrace),
                selectedTrace,
                "cases=" + total + ", passed=" + passed + ", failed=" + failed,
                signals,
                null,
                null,
                null,
                null,
                null,
                ok,
                score,
                null,
                ok ? null : "One or more eval cases failed.",
                ok ? null : "evaluation_failed"
        ));
    }

    public RuntimeRunRecord recordGaiaRun(String runId, GaiaRunResult result, Instant startedAt, Instant endedAt) {
        List<String> selectedTrace = new ArrayList<>();
        selectedTrace.add("chapter10-run-start:" + runId);
        selectedTrace.add("chapter10-lane:gaia");
        selectedTrace.add("gaia:passed:" + result.passed());
        selectedTrace.add("gaia:failed:" + result.failed());
        selectedTrace.add(result.failed() == 0 ? "chapter10-run-complete:" + runId : "chapter10-run-failed:" + runId);
        return historyStore.record(new RuntimeRunRecord(
                runId,
                "gaia",
                "validation",
                null,
                null,
                null,
                result.datasetUrl() == null || result.datasetUrl().isBlank() ? result.localPath() : result.datasetUrl(),
                compactInput(result.datasetUrl() == null || result.datasetUrl().isBlank() ? result.localPath() : result.datasetUrl()),
                startedAt,
                endedAt,
                duration(startedAt, endedAt),
                result.failed() == 0 ? "COMPLETED" : "REJECTED",
                result.failed() == 0 ? "passed" : "failed",
                "GAIA validation run " + runId + " completed with " + result.passed() + " passed and " + result.failed() + " failed cases.",
                compactTrace(selectedTrace),
                selectedTrace,
                "cases=" + result.total() + ", passed=" + result.passed() + ", failed=" + result.failed(),
                List.of(
                        "total:" + result.total(),
                        "passed:" + result.passed(),
                        "failed:" + result.failed(),
                        "durationMs:" + result.durationMillis()
                ),
                null,
                null,
                null,
                null,
                null,
                result.failed() == 0,
                result.total() == 0 ? null : ((double) result.passed() / (double) result.total()),
                null,
                result.failed() == 0 ? null : "GAIA validation reported failed cases.",
                result.failed() == 0 ? null : "gaia_validation_failed"
        ));
    }

    private List<String> selectedEntries(List<String> trace, int limit) {
        if (trace == null || trace.isEmpty()) {
            return List.of();
        }
        return trace.stream().filter(Objects::nonNull).limit(limit).toList();
    }

    private String compactTrace(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }
        return String.join(" | ", entries);
    }

    private String compactInput(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.length() <= 160) {
            return trimmed;
        }
        return trimmed.substring(0, 157) + "...";
    }

    private String toolUsageSummary(List<String> trace, int toolCount) {
        int effectiveToolCount = toolCount;
        if ((trace != null && !trace.isEmpty()) && effectiveToolCount <= 0) {
            effectiveToolCount = (int) trace.stream().filter(entry -> entry != null && entry.contains("tool")).count();
        }
        return "tools=" + effectiveToolCount;
    }

    private int countMatches(List<String> trace, String token) {
        if (trace == null || trace.isEmpty()) {
            return 0;
        }
        return (int) trace.stream().filter(entry -> entry != null && entry.contains(token)).count();
    }

    private long duration(Instant startedAt, Instant endedAt) {
        return startedAt == null || endedAt == null ? 0L : Math.max(0L, Duration.between(startedAt, endedAt).toMillis());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String statusFromStopReason(dk.ashlan.agent.core.StopReason stopReason) {
        if (stopReason == null) {
            return "COMPLETED";
        }
        return switch (stopReason) {
            case FINAL_ANSWER, PENDING_CONFIRMATION -> "COMPLETED";
            case MAX_ITERATIONS, REFLECTION_REJECTED -> "REJECTED";
            case TOOL_ERROR -> "FAILED";
        };
    }
}
