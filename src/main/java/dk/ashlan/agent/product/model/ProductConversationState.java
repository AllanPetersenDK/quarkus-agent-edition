package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Persistent product conversation state used for operator inspection and future identity binding.")
public record ProductConversationState(
        @Schema(description = "Stable conversation identifier.")
        String conversationId,
        @Schema(description = "Conversation creation timestamp.")
        Instant createdAt,
        @Schema(description = "Conversation last-update timestamp.")
        Instant updatedAt,
        @Schema(description = "Stored product turns for the conversation.")
        List<ProductConversationTurn> turns
) {
    public ProductConversationState {
        turns = turns == null ? List.of() : List.copyOf(turns);
    }

    public int turnCount() {
        return turns.size();
    }

    public ProductConversationTurn lastTurn() {
        return turns.isEmpty() ? null : turns.get(turns.size() - 1);
    }

    public String lastRunId() {
        return lastTurn() == null ? null : lastTurn().runId();
    }

    public String lastStatus() {
        return lastTurn() == null ? null : lastTurn().status();
    }

    public String lastQuery() {
        return lastTurn() == null ? null : lastTurn().query();
    }

    public String lastAnswer() {
        return lastTurn() == null ? null : lastTurn().answer();
    }

    public String lastFailureReason() {
        return lastTurn() == null ? null : lastTurn().failureReason();
    }

    public String lastSummary() {
        return lastTurn() == null ? null : lastTurn().summary();
    }

    public String lastTraceSummary() {
        return lastTurn() == null ? null : lastTurn().traceSummary();
    }

    public String lastToolUsageSummary() {
        return lastTurn() == null ? null : lastTurn().toolUsageSummary();
    }

    public String lastPlanSummary() {
        return lastTurn() == null ? null : lastTurn().planSummary();
    }

    public String lastReflectionSummary() {
        return lastTurn() == null ? null : lastTurn().reflectionSummary();
    }

    public Instant lastStartedAt() {
        return lastTurn() == null ? null : lastTurn().startedAt();
    }

    public Instant lastCompletedAt() {
        return lastTurn() == null ? null : lastTurn().completedAt();
    }

    public long lastDurationMs() {
        return lastTurn() == null ? 0L : lastTurn().durationMs();
    }

    public int lastArtifactCount() {
        return lastTurn() == null ? 0 : lastTurn().artifacts().size();
    }

    public List<ProductArtifactSummaryResponse> lastArtifacts() {
        return lastTurn() == null ? List.of() : lastTurn().artifacts();
    }

    public List<ProductArtifactSummaryResponse> allArtifacts() {
        if (turns.isEmpty()) {
            return List.of();
        }
        return turns.stream()
                .flatMap(turn -> turn.artifacts().stream())
                .toList();
    }

    public List<String> lastQualitySignals() {
        return lastTurn() == null ? List.of() : lastTurn().qualitySignals();
    }

    public Integer lastSourceCount() {
        return lastTurn() == null ? null : lastTurn().sourceCount();
    }

    public Integer lastCitationCount() {
        return lastTurn() == null ? null : lastTurn().citationCount();
    }

    public Integer lastRetrievalCount() {
        return lastTurn() == null ? null : lastTurn().retrievalCount();
    }

    public Integer lastPlanStepCount() {
        return lastTurn() == null ? null : lastTurn().planStepCount();
    }
}
