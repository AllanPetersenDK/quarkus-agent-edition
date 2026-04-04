package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Locale;

/**
 * Cross-session memory service.
 *
 * <p>Conversation history and session state are owned by {@link SessionManager} and
 * {@link SessionState}. This service only stores compact memory signals that can be
 * searched later across sessions.</p>
 */
@ApplicationScoped
public class MemoryService {
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
        return memoryStore.findRelevant(sessionId, query, 3).stream().map(TaskMemory::memory).toList();
    }

    public List<TaskMemory> longTermMemories(String sessionId, String query, int limit) {
        return memoryStore.findRelevant(sessionId, query, limit);
    }

    private boolean isDuplicate(TaskMemory candidate) {
        String normalizedCandidate = normalize(candidate.memory());
        if (normalizedCandidate.isBlank()) {
            return true;
        }
        return memoryStore.findRelevant(candidate.sessionId(), normalizedCandidate, 10).stream()
                .anyMatch(existing -> normalize(existing.memory()).equals(normalizedCandidate));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
