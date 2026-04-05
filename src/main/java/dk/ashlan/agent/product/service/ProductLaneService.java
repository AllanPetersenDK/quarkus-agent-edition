package dk.ashlan.agent.product.service;

import dk.ashlan.agent.eval.RuntimeRunHistoryStore;
import dk.ashlan.agent.eval.RuntimeRunRecord;
import dk.ashlan.agent.product.api.ProductApiException;
import dk.ashlan.agent.product.model.ProductArtifactCollectionResponse;
import dk.ashlan.agent.product.model.ProductArtifactSummaryResponse;
import dk.ashlan.agent.product.model.ProductConversationDetailResponse;
import dk.ashlan.agent.product.model.ProductConversationState;
import dk.ashlan.agent.product.model.ProductConversationSummaryResponse;
import dk.ashlan.agent.product.model.ProductPlanResponse;
import dk.ashlan.agent.product.model.ProductOverviewHealthSummaryResponse;
import dk.ashlan.agent.product.model.ProductOverviewResponse;
import dk.ashlan.agent.product.model.ProductRunDetailResponse;
import dk.ashlan.agent.product.model.ProductRunSummaryResponse;
import dk.ashlan.agent.product.model.ProductConversationTurn;
import dk.ashlan.agent.product.model.ProductReflectionResponse;
import dk.ashlan.agent.product.model.ProductSourceResponse;
import dk.ashlan.agent.product.store.ProductConversationStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class ProductLaneService {
    private final ProductConversationStore conversationStore;
    private final RuntimeRunHistoryStore runHistoryStore;

    @Inject
    public ProductLaneService(ProductConversationStore conversationStore, RuntimeRunHistoryStore runHistoryStore) {
        this.conversationStore = conversationStore;
        this.runHistoryStore = runHistoryStore;
    }

    public List<ProductConversationSummaryResponse> listConversations(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return conversationStore.list(limit).stream()
                .map(ProductConversationSummaryResponse::from)
                .toList();
    }

    public ProductConversationDetailResponse conversation(String conversationId) {
        return conversationStore.load(conversationId)
                .map(ProductConversationDetailResponse::from)
                .orElseThrow(() -> new ProductApiException(404, "product_conversation_not_found", "No product conversation found for conversationId=" + conversationId, conversationId, null, null));
    }

    public ProductOverviewResponse overview(int recentLimit) {
        int safeLimit = Math.max(1, recentLimit);
        List<ProductConversationState> recentConversationStates = conversationStore.list(safeLimit);
        List<ProductConversationSummaryResponse> recentConversations = recentConversationStates.stream()
                .map(ProductConversationSummaryResponse::from)
                .toList();

        List<ProductRunSummaryResponse> allRunSummaries = conversationStore.list(Integer.MAX_VALUE).stream()
                .flatMap(state -> state.turns().stream().map(turn -> toRunSummary(state, turn)))
                .sorted(runComparator())
                .toList();
        if (allRunSummaries.isEmpty() && runHistoryStore != null) {
            allRunSummaries = runHistoryStore.list("product", Integer.MAX_VALUE).stream()
                    .map(this::toRunSummaryFromHistory)
                    .sorted(runComparator())
                    .toList();
        }
        List<ProductRunSummaryResponse> recentRuns = allRunSummaries.stream()
                .limit(safeLimit)
                .toList();
        List<ProductRunSummaryResponse> recentFailures = allRunSummaries.stream()
                .filter(run -> isFailure(run.status()))
                .limit(safeLimit)
                .toList();

        List<ProductArtifactSummaryResponse> recentArtifacts = recentConversationStates.stream()
                .flatMap(state -> state.allArtifacts().stream())
                .sorted(Comparator.comparing(ProductArtifactSummaryResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();

        long totalConversations = conversationStore.count();
        long totalRuns = allRunSummaries.size();
        long activeRuns = allRunSummaries.stream().filter(run -> isActive(run.status())).count();
        long completedRuns = allRunSummaries.stream().filter(run -> "COMPLETED".equalsIgnoreCase(run.status())).count();
        long failedRuns = allRunSummaries.stream().filter(run -> isFailure(run.status())).count();

        ProductConversationSummaryResponse latestConversation = recentConversations.isEmpty() ? null : recentConversations.get(0);
        String latestRunId = allRunSummaries.isEmpty() ? null : allRunSummaries.get(0).runId();
        String latestStatus = latestConversation == null ? null : latestConversation.lastStatus();
        ProductOverviewHealthSummaryResponse health = new ProductOverviewHealthSummaryResponse(
                failedRuns == 0 ? "UP" : "DEGRADED",
                "Product lane is " + (failedRuns == 0 ? "healthy" : "serving recent failures"),
                totalConversations,
                totalRuns,
                failedRuns
        );
        List<String> signals = new ArrayList<>();
        signals.add("conversationCount:" + totalConversations);
        signals.add("runCount:" + totalRuns);
        signals.add("activeRuns:" + activeRuns);
        signals.add("completedRuns:" + completedRuns);
        signals.add("failedRuns:" + failedRuns);
        signals.add("recentArtifacts:" + recentArtifacts.size());
        if (latestConversation != null) {
            signals.add("latestConversation:" + latestConversation.conversationId());
            signals.add("latestStatus:" + latestConversation.lastStatus());
        }

        return new ProductOverviewResponse(
                totalConversations,
                totalRuns,
                activeRuns,
                completedRuns,
                failedRuns,
                recentConversations.size(),
                recentRuns.size(),
                recentFailures.size(),
                recentArtifacts.size(),
                latestConversation == null ? null : latestConversation.conversationId(),
                latestRunId,
                latestStatus,
                latestConversation == null ? null : latestConversation.updatedAt(),
                health,
                recentConversations,
                recentRuns,
                recentFailures,
                recentArtifacts,
                List.copyOf(signals)
        );
    }

    public ProductRunDetailResponse run(String runId) {
        ProductRunContext context = findRunContext(runId);
        RuntimeRunRecord record = context.record();
        ProductConversationTurn turn = context.turn();
        List<ProductArtifactSummaryResponse> artifacts = turn == null ? List.of() : turn.artifacts();
        List<ProductSourceResponse> sources = turn == null ? List.of() : turn.sources();
        List<String> traceHighlights = traceHighlights(record, turn);
        String planSummary = turn == null ? null : turn.planSummary();
        String reflectionSummary = turn == null ? null : turn.reflectionSummary();
        ProductPlanResponse plan = turn == null ? null : turn.plan();
        ProductReflectionResponse reflection = turn == null ? null : turn.reflection();
        String summary = turn == null ? summarizeRun(record) : turn.summary();
        return new ProductRunDetailResponse(
                record.runId(),
                record.conversationId(),
                record.objective(),
                record.outcome(),
                record.inputSummary(),
                record.outcome(),
                summary,
                record.status(),
                record.startTime(),
                record.endTime(),
                record.durationMs(),
                record.traceSummary(),
                record.toolUsageSummary(),
                traceHighlights,
                record.outcomeCategory(),
                planSummary,
                reflectionSummary,
                plan,
                reflection,
                sources,
                artifacts,
                record.qualitySignals(),
                safeCount(record.sourceCount()),
                safeCount(record.citationCount()),
                safeCount(record.retrievalCount()),
                safeCount(record.toolCount()),
                safeCount(record.planStepCount()),
                artifacts.size(),
                record.approved(),
                record.score(),
                record.rejectionReason(),
                record.failureReason(),
                record.errorCategory()
        );
    }

    public ProductArtifactCollectionResponse runArtifacts(String runId) {
        ProductRunContext context = findRunContext(runId);
        RuntimeRunRecord record = context.record();
        ProductConversationTurn turn = context.turn();
        List<ProductArtifactSummaryResponse> artifacts = turn == null ? List.of() : turn.artifacts();
        return new ProductArtifactCollectionResponse(record.runId(), record.conversationId(), artifacts.size(), artifacts);
    }

    private ProductRunContext findRunContext(String runId) {
        ProductConversationState state = conversationStore.list(Integer.MAX_VALUE).stream()
                .filter(candidate -> candidate.turns().stream().anyMatch(turn -> runId.equals(turn.runId())))
                .findFirst()
                .orElse(null);
        if (state != null) {
            ProductConversationTurn turn = state.turns().stream()
                    .filter(candidate -> runId.equals(candidate.runId()))
                    .findFirst()
                    .orElse(null);
            if (turn != null) {
                RuntimeRunRecord record = runHistoryStore == null ? null : runHistoryStore.find(runId).filter(found -> "product".equalsIgnoreCase(found.lane())).orElse(null);
                if (record == null) {
                    record = new RuntimeRunRecord(
                            turn.runId(),
                            "product",
                            "query",
                            null,
                            state.conversationId(),
                            null,
                            turn.query(),
                            turn.query(),
                            turn.startedAt(),
                            turn.completedAt(),
                            turn.durationMs(),
                            turn.status(),
                            turn.status().equalsIgnoreCase("COMPLETED") ? "answered_with_sources" : "reflection_rejected",
                            turn.answer(),
                            turn.traceSummary(),
                            turn.qualitySignals(),
                            turn.toolUsageSummary(),
                            turn.traceHighlights(),
                            turn.sourceCount(),
                            turn.citationCount(),
                            turn.retrievalCount(),
                            4,
                            turn.planStepCount(),
                            turn.approved(),
                            turn.score(),
                            turn.rejectionReason(),
                            turn.failureReason(),
                            turn.errorCategory()
                    );
                }
                return new ProductRunContext(record, turn);
            }
        }
        RuntimeRunRecord record = runHistoryStore.find(runId)
                .filter(found -> "product".equalsIgnoreCase(found.lane()))
                .orElseThrow(() -> new ProductApiException(404, "product_run_not_found", "No product run found for runId=" + runId, null, runId, null));
        return new ProductRunContext(record, null);
    }

    private ProductRunSummaryResponse toRunSummary(ProductConversationState state, ProductConversationTurn turn) {
        RuntimeRunRecord record = runHistoryStore == null ? null : runHistoryStore.find(turn.runId()).filter(found -> "product".equalsIgnoreCase(found.lane())).orElse(null);
        if (record == null) {
            record = new RuntimeRunRecord(
                    turn.runId(),
                    "product",
                    "query",
                    null,
                    state.conversationId(),
                    null,
                    turn.query(),
                    turn.query(),
                    turn.startedAt(),
                    turn.completedAt(),
                    turn.durationMs(),
                    turn.status(),
                    turn.status().equalsIgnoreCase("COMPLETED") ? "answered_with_sources" : "reflection_rejected",
                            turn.answer(),
                            turn.traceSummary(),
                            turn.qualitySignals(),
                            turn.toolUsageSummary(),
                            turn.traceHighlights(),
                            turn.sourceCount(),
                            turn.citationCount(),
                            turn.retrievalCount(),
                            4,
                            turn.planStepCount(),
                            turn.approved(),
                            turn.score(),
                            turn.rejectionReason(),
                            turn.failureReason(),
                            turn.errorCategory()
                    );
        }
        int artifactCount = turn == null ? 0 : turn.artifacts().size();
        String planSummary = turn.planSummary();
        String reflectionSummary = turn.reflectionSummary();
        List<String> traceHighlights = traceHighlights(record, turn);
        return new ProductRunSummaryResponse(
                record.runId(),
                record.conversationId(),
                record.status(),
                record.inputSummary(),
                record.outcome(),
                turn == null ? summarizeRun(record) : turn.summary(),
                record.startTime(),
                record.endTime(),
                record.durationMs(),
                record.traceSummary(),
                record.toolUsageSummary(),
                traceHighlights,
                record.outcomeCategory(),
                planSummary,
                reflectionSummary,
                safeCount(record.sourceCount()),
                safeCount(record.citationCount()),
                safeCount(record.retrievalCount()),
                safeCount(record.toolCount()),
                safeCount(record.planStepCount()),
                artifactCount,
                record.qualitySignals(),
                record.approved(),
                record.score(),
                record.rejectionReason(),
                record.failureReason(),
                record.errorCategory()
        );
    }

    private ProductRunSummaryResponse toRunSummaryFromHistory(RuntimeRunRecord record) {
        return new ProductRunSummaryResponse(
                record.runId(),
                record.conversationId(),
                record.status(),
                record.inputSummary(),
                record.outcome(),
                summarizeRun(record),
                record.startTime(),
                record.endTime(),
                record.durationMs(),
                record.traceSummary(),
                record.toolUsageSummary(),
                record.selectedTraceEntries(),
                record.outcomeCategory(),
                null,
                null,
                safeCount(record.sourceCount()),
                safeCount(record.citationCount()),
                safeCount(record.retrievalCount()),
                safeCount(record.toolCount()),
                safeCount(record.planStepCount()),
                0,
                record.qualitySignals(),
                record.approved(),
                record.score(),
                record.rejectionReason(),
                record.failureReason(),
                record.errorCategory()
        );
    }

    private Comparator<ProductRunSummaryResponse> runComparator() {
        return Comparator.comparing(ProductRunSummaryResponse::startedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static boolean isFailure(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        return normalized.equals("FAILED") || normalized.equals("REJECTED");
    }

    private static boolean isActive(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        return normalized.equals("RUNNING") || normalized.equals("PENDING") || normalized.equals("IN_PROGRESS");
    }

    private static int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private List<String> traceHighlights(RuntimeRunRecord record, ProductConversationTurn turn) {
        if (turn != null && turn.traceHighlights() != null && !turn.traceHighlights().isEmpty()) {
            return turn.traceHighlights();
        }
        if (record != null && record.selectedTraceEntries() != null && !record.selectedTraceEntries().isEmpty()) {
            return record.selectedTraceEntries();
        }
        return List.of();
    }

    private record ProductRunContext(RuntimeRunRecord record, ProductConversationTurn turn) {
    }

    private String summarizeRun(RuntimeRunRecord record) {
        return record.outcome() == null || record.outcome().isBlank()
                ? record.status()
                : record.outcome();
    }
}
