package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Cross-session memory service.
 *
 * <p>Conversation history and session state are owned by {@link SessionManager} and
 * {@link SessionState}. This service only stores compact memory signals that can be
 * searched later across sessions.</p>
 */
@ApplicationScoped
public class MemoryService {
    private static final String EPHEMERAL_SESSION_PREFIX = "ephemeral-";
    private static final double NEAR_DUPLICATE_OVERLAP_THRESHOLD = 0.75d;
    private final TaskMemoryStore memoryStore;
    private final MemoryExtractionService extractionService;

    @Inject
    public MemoryService(TaskMemoryStore memoryStore, MemoryExtractionService extractionService) {
        this.memoryStore = memoryStore;
        this.extractionService = extractionService;
    }

    /**
     * Compatibility constructor kept for older demo/support paths.
     * Prefer {@link #MemoryService(TaskMemoryStore, MemoryExtractionService)} so the
     * separation between session state and long-term memory stays explicit.
     */
    @Deprecated(forRemoval = false)
    public MemoryService(SessionManager sessionManager, TaskMemoryStore memoryStore, MemoryExtractionService extractionService) {
        this(memoryStore, extractionService);
    }

    public MemoryWriteDecision remember(String sessionId, String task, String message) {
        if (isEphemeralSession(sessionId)) {
            return MemoryWriteDecision.SKIP;
        }
        MemoryExtractionResult extraction = extractionService.extract(sessionId, task, message);
        if (extraction.decision() == MemoryWriteDecision.SKIP || extraction.memory() == null) {
            return MemoryWriteDecision.SKIP;
        }
        if (isDuplicate(extraction.memory())) {
            return MemoryWriteDecision.SKIP;
        }
        memoryStore.save(extraction.memory());
        return MemoryWriteDecision.ADD;
    }

    public List<String> relevantMemories(String sessionId, String query) {
        if (isEphemeralSession(sessionId)) {
            return List.of();
        }
        return memoryStore.findRelevant(sessionId, query, 3).stream().map(TaskMemory::memory).toList();
    }

    public List<TaskMemory> longTermMemories(String sessionId, String query, int limit) {
        if (isEphemeralSession(sessionId)) {
            return List.of();
        }
        return memoryStore.findRelevant(sessionId, query, limit);
    }

    private boolean isDuplicate(TaskMemory candidate) {
        String candidateKey = candidate.structuredDedupKey();
        if (candidateKey.isBlank()) {
            return true;
        }
        return memoryStore.findRelevant(candidate.sessionId(), candidateKey, 25).stream()
                .anyMatch(existing -> existing != null
                        && (existing.structuredDedupKey().equals(candidateKey)
                        || tokenOverlap(existing, candidate) >= NEAR_DUPLICATE_OVERLAP_THRESHOLD));
    }

    private double tokenOverlap(TaskMemory first, TaskMemory second) {
        List<String> firstTokens = first.dedupTokens();
        List<String> secondTokens = second.dedupTokens();
        if (firstTokens.isEmpty() || secondTokens.isEmpty()) {
            return 0.0d;
        }
        long matches = secondTokens.stream().filter(firstTokens::contains).count();
        return (double) matches / (double) Math.max(firstTokens.size(), secondTokens.size());
    }

    private boolean isEphemeralSession(String sessionId) {
        return sessionId == null || sessionId.isBlank() || sessionId.startsWith(EPHEMERAL_SESSION_PREFIX);
    }
}
