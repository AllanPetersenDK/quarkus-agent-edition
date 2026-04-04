package dk.ashlan.agent.eval.gaia;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.eval.AgentTraceService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Legacy starter harness for chapter-10 GAIA validation.
 * <p>
 * The canonical GAIA validation/dev seam is {@link dk.ashlan.agent.api.GaiaEvaluationResource}
 * and {@link GaiaValidationRunner}. This class is retained for backward-compatible chapter
 * demos and comparison tests.
 */
@Deprecated(forRemoval = false)
@ApplicationScoped
public class GaiaEvaluationRunner {
    private final AgentOrchestrator agentOrchestrator;
    private final GaiaDatasetLoader datasetLoader;
    private final AgentTraceService traceService;
    private final GaiaExactMatchScorer scorer = new GaiaExactMatchScorer();
    private final GaiaSummaryCalculator summaryCalculator = new GaiaSummaryCalculator();
    private final int defaultLimit;

    @Inject
    public GaiaEvaluationRunner(
            AgentOrchestrator agentOrchestrator,
            GaiaDatasetLoader datasetLoader,
            AgentTraceService traceService,
            @ConfigProperty(name = "gaia.validation.default-limit", defaultValue = "10") int defaultLimit
    ) {
        this.agentOrchestrator = agentOrchestrator;
        this.datasetLoader = datasetLoader;
        this.traceService = traceService;
        this.defaultLimit = defaultLimit;
    }

    public GaiaValidationResponse run(int limit) {
        List<GaiaValidationCase> loaded = datasetLoader.load();
        List<GaiaValidationCase> filtered = GaiaCaseFilter.level1WithoutAttachments(loaded);
        int requestedLimit = limit > 0 ? limit : defaultLimit;
        List<GaiaValidationCase> selected = filtered.stream().limit(requestedLimit).toList();
        String runId = UUID.randomUUID().toString();
        List<GaiaValidationCaseResult> results = new ArrayList<>();
        for (GaiaValidationCase validationCase : selected) {
            results.add(runCase(runId, validationCase));
        }
        GaiaValidationSummary summary = summary(results);
        return new GaiaValidationResponse(datasetLoader.datasetUrl(), requestedLimit, loaded.size(), selected.size(), summary, List.copyOf(results));
    }

    public GaiaValidationSummary summary(List<GaiaValidationCaseResult> results) {
        return summaryCalculator.summarize(results);
    }

    private GaiaValidationCaseResult runCase(String runId, GaiaValidationCase validationCase) {
        String sessionId = "gaia-" + runId + "-" + validationCase.taskId();
        AgentRunResult output = agentOrchestrator.run(validationCase.question(), sessionId);
        boolean correct = scorer.matches(validationCase.finalAnswer(), output.finalAnswer());
        List<String> events = new ArrayList<>(output.trace());
        events.add("taskId:" + validationCase.taskId());
        events.add("question:" + validationCase.question());
        events.add("expected:" + scorer.normalize(validationCase.finalAnswer()));
        events.add("predicted:" + scorer.normalize(output.finalAnswer()));
        events.add("correct:" + correct);
        events.add("iterations:" + output.iterations());
        events.add("stopReason:" + (output.stopReason() == null ? "" : output.stopReason().name()));
        traceService.record(validationCase.taskId(), events);
        return new GaiaValidationCaseResult(
                validationCase.taskId(),
                validationCase.question(),
                validationCase.finalAnswer(),
                output.finalAnswer(),
                correct,
                output.iterations(),
                output.stopReason() == null ? null : output.stopReason().name(),
                List.copyOf(output.trace())
        );
    }
}
