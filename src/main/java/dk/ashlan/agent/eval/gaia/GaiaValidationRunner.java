package dk.ashlan.agent.eval.gaia;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.llm.LlmMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class GaiaValidationRunner {
    private final AgentOrchestrator agentOrchestrator;
    private final GaiaDatasetService datasetService;
    private final GaiaEvalCaseMapper caseMapper;
    private final GaiaAnswerScorer scorer;
    private final GaiaQuestionClassifier questionClassifier;
    private final GaiaAnswerPolicy answerPolicy;
    private final GaiaAnswerPostProcessor answerPostProcessor;
    private final GaiaAttachmentResolver attachmentResolver;
    private final GaiaEvaluationStore evaluationStore;
    private final Config config;
    private final int defaultLimit;
    private final int defaultLevel;
    private final String defaultConfig;
    private final String defaultSplit;

    @Inject
    public GaiaValidationRunner(
            AgentOrchestrator agentOrchestrator,
            GaiaDatasetService datasetService,
            GaiaEvalCaseMapper caseMapper,
            GaiaAnswerScorer scorer,
            GaiaAttachmentResolver attachmentResolver,
            GaiaEvaluationStore evaluationStore,
            Config config
    ) {
        this.agentOrchestrator = agentOrchestrator;
        this.datasetService = datasetService;
        this.caseMapper = caseMapper;
        this.scorer = scorer;
        this.questionClassifier = new GaiaQuestionClassifier();
        this.answerPolicy = new GaiaAnswerPolicy();
        this.answerPostProcessor = new GaiaAnswerPostProcessor(scorer);
        this.attachmentResolver = attachmentResolver;
        this.evaluationStore = evaluationStore;
        this.config = config;
        this.defaultLimit = config.getOptionalValue("gaia.validation.default-limit", Integer.class).orElse(10);
        this.defaultLevel = config.getOptionalValue("gaia.validation.default-level", Integer.class).orElse(1);
        this.defaultConfig = config.getOptionalValue("gaia.validation.default-config", String.class).orElse("2023");
        this.defaultSplit = config.getOptionalValue("gaia.validation.default-split", String.class).orElse("validation");
    }

    public GaiaRunResult run(GaiaRunRequest request) {
        String configuredDatasetUrl = config.getOptionalValue("gaia.validation.dataset-url", String.class).orElse("");
        String configuredLocalPath = config.getOptionalValue("gaia.validation.local-path", String.class).orElse("");
        String datasetUrl = request == null || request.datasetUrl() == null || request.datasetUrl().isBlank() ? configuredDatasetUrl : value(request.datasetUrl());
        String localPath = request == null || request.localPath() == null || request.localPath().isBlank() ? configuredLocalPath : value(request.localPath());
        String configName = request == null || request.config() == null || request.config().isBlank() ? defaultConfig : value(request.config());
        String split = request == null || request.split() == null || request.split().isBlank() ? defaultSplit : value(request.split());
        Integer level = request == null ? null : request.level();
        int effectiveLevel = level == null ? defaultLevel : request.effectiveLevel(defaultLevel);
        int effectiveLimit = request == null ? defaultLimit : request.effectiveLimit(defaultLimit);
        boolean failFast = request != null && request.isFailFast();

        GaiaDatasetSelection selection = new GaiaDatasetSelection(datasetUrl, localPath, configName, split, effectiveLevel, effectiveLimit, failFast);
        List<GaiaExample> loaded = datasetService.load(selection);
        String runId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        List<GaiaCaseResult> results = new ArrayList<>();
        for (GaiaExample example : loaded) {
            GaiaCaseResult caseResult = runCase(runId, example);
            results.add(caseResult);
            if (failFast && !caseResult.correct()) {
                break;
            }
        }
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        GaiaRunResult runResult = summarize(runId, selection, results, durationMillis);
        evaluationStore.save(runResult);
        return runResult;
    }

    public GaiaCaseResult trace(String taskId) {
        return evaluationStore.findCase(taskId)
                .orElseThrow(() -> new IllegalArgumentException("No GAIA case found for taskId=" + taskId));
    }

    public GaiaRunResult runResult(String runId) {
        return evaluationStore.findRun(runId)
                .orElseThrow(() -> new IllegalArgumentException("No GAIA run found for runId=" + runId));
    }

    private GaiaCaseResult runCase(String runId, GaiaExample example) {
        GaiaEvalCase evalCase = caseMapper.map(example);
        String sessionId = "gaia-" + runId + "-" + evalCase.taskId();
        GaiaQuestionType questionType = questionClassifier.classify(evalCase.prompt());
        List<String> attachmentEvents = example.attachment() == null ? List.of() : example.attachment().traceEvents();
        List<LlmMessage> supplementalMessages = new ArrayList<>();
        List<String> policyTraceEvents = new ArrayList<>();
        policyTraceEvents.add("gaia-question-type:" + questionType.name());
        if (questionType == GaiaQuestionType.SINGLE_ENTITY) {
            supplementalMessages.addAll(answerPolicy.supplementalMessages(questionType));
            policyTraceEvents.add("gaia-answer-policy:single-entity");
        }
        if (example.attachment() != null) {
            String note = attachmentResolver.toContextNote(example.attachment());
            if (!note.isBlank()) {
                supplementalMessages.add(LlmMessage.system(note));
            }
        }
        AgentRunResult output = supplementalMessages.isEmpty()
                ? agentOrchestrator.run(evalCase.prompt(), sessionId)
                : agentOrchestrator.run(evalCase.prompt(), sessionId, supplementalMessages);
        GaiaAnswerPostProcessResult postProcessed = answerPostProcessor.process(questionType, evalCase.expectedAnswers(), output.finalAnswer());
        GaiaScoreResult score = scorer.score(evalCase.prompt(), evalCase.expectedAnswers(), postProcessed.answer());
        List<String> trace = new ArrayList<>(output.trace());
        trace.addAll(policyTraceEvents);
        trace.add("gaia-answer-raw:" + (output.finalAnswer() == null ? "" : output.finalAnswer()));
        trace.addAll(postProcessed.traceEvents());
        trace.add("gaia-answer-final:" + postProcessed.answer());
        trace.add("taskId:" + evalCase.taskId());
        trace.add("question:" + evalCase.prompt());
        trace.add("expectedNormalized:" + score.expectedNormalized());
        trace.add("predictedNormalized:" + score.predictedNormalized());
        trace.add("scoreReason:" + score.reason());
        trace.add("scoreValue:" + score.score());
        trace.add("correct:" + score.passed());
        trace.add("iterations:" + output.iterations());
        trace.add("stopReason:" + (output.stopReason() == null ? "" : output.stopReason().name()));
        if (example.attachment() != null) {
            trace.addAll(example.attachment().traceEvents());
        }
        return new GaiaCaseResult(
                runId,
                evalCase.taskId(),
                evalCase.prompt(),
                evalCase.level(),
                evalCase.expectedAnswers(),
                postProcessed.answer(),
                score,
                score.passed(),
                output.iterations(),
                output.stopReason() == null ? null : output.stopReason().name(),
                List.copyOf(trace),
                List.copyOf(attachmentEvents),
                example.attachment(),
                evalCase.sourceMetadata()
        );
    }

    private GaiaRunResult summarize(String runId, GaiaDatasetSelection selection, List<GaiaCaseResult> results, long durationMillis) {
        int total = results.size();
        int passed = (int) results.stream().filter(GaiaCaseResult::correct).count();
        int failed = total - passed;
        GaiaLevelSummary summary = summaryFor(results);
        Map<String, GaiaLevelSummary> byLevel = results.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        GaiaCaseResult::level,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), this::summaryFor)
                ));
        List<GaiaCaseResult> failures = results.stream()
                .filter(result -> !result.correct())
                .sorted(Comparator.comparing(GaiaCaseResult::taskId))
                .limit(5)
                .toList();
        return new GaiaRunResult(
                runId,
                selection.datasetUrl(),
                selection.localPath(),
                selection.config(),
                selection.split(),
                selection.level(),
                selection.limit(),
                total,
                passed,
                failed,
                durationMillis,
                summary,
                byLevel,
                failures,
                List.copyOf(results)
        );
    }

    private GaiaLevelSummary summaryFor(List<GaiaCaseResult> results) {
        int total = results.size();
        int passed = (int) results.stream().filter(GaiaCaseResult::correct).count();
        int failed = total - passed;
        double accuracy = total == 0 ? 0.0 : (double) passed / (double) total;
        List<Integer> iterations = results.stream()
                .map(GaiaCaseResult::iterations)
                .filter(value -> value != null && value > 0)
                .toList();
        Double averageIterations = iterations.isEmpty()
                ? null
                : iterations.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        return new GaiaLevelSummary(total, passed, failed, accuracy, averageIterations);
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}
