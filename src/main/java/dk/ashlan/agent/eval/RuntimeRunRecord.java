package dk.ashlan.agent.eval;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Small chapter-10 runtime run observation that makes execution history inspectable across lanes.")
public record RuntimeRunRecord(
        @Schema(description = "Stable run identifier.")
        String runId,
        @Schema(description = "Broad lane such as manual, product, code, multi-agent, evaluation, or gaia.")
        String lane,
        @Schema(description = "Specific run type within the lane, such as run, step, query, or case-eval.")
        String runType,
        @Schema(description = "Session identifier, when the lane is session-scoped.")
        String sessionId,
        @Schema(description = "Conversation identifier, when the lane is conversation-scoped.")
        String conversationId,
        @Schema(description = "Evaluation case identifier, when the run originated from a named case.")
        String caseId,
        @Schema(description = "Primary input or objective that triggered the run.")
        String objective,
        @Schema(description = "Compact input summary for inspection views.")
        String inputSummary,
        @Schema(description = "Run start timestamp.")
        Instant startTime,
        @Schema(description = "Run end timestamp.")
        Instant endTime,
        @Schema(description = "Wall-clock duration in milliseconds.")
        long durationMs,
        @Schema(description = "Execution status such as COMPLETED, REJECTED, or FAILED.")
        String status,
        @Schema(description = "Outcome category such as approved, rejected, passed, failed, validation_passed, or empty_knowledge.")
        String outcomeCategory,
        @Schema(description = "Human-readable outcome summary.")
        String outcome,
        @Schema(description = "Compact trace summary for quick inspection.")
        String traceSummary,
        @Schema(description = "Selected trace excerpts that explain the run without the original response body.")
        List<String> selectedTraceEntries,
        @Schema(description = "Compact tool-usage summary.")
        String toolUsageSummary,
        @Schema(description = "Quality signals that were observed for the run.")
        List<String> qualitySignals,
        @Schema(description = "Number of sources used when relevant.")
        Integer sourceCount,
        @Schema(description = "Number of citations used when relevant.")
        Integer citationCount,
        @Schema(description = "Number of retrieval hits when relevant.")
        Integer retrievalCount,
        @Schema(description = "Number of tools or tool-like steps used when relevant.")
        Integer toolCount,
        @Schema(description = "Number of planning steps when relevant.")
        Integer planStepCount,
        @Schema(description = "Approval status when a reviewer or quality gate applies.")
        Boolean approved,
        @Schema(description = "Simple score when the lane computes one.")
        Double score,
        @Schema(description = "Reason a review or quality gate rejected the run, if any.")
        String rejectionReason,
        @Schema(description = "Reason a run failed, if any.")
        String failureReason,
        @Schema(description = "Simple error category for failures, if any.")
        String errorCategory
) {
    public RuntimeRunRecord {
        selectedTraceEntries = selectedTraceEntries == null ? List.of() : List.copyOf(selectedTraceEntries);
        qualitySignals = qualitySignals == null ? List.of() : List.copyOf(qualitySignals);
    }
}
