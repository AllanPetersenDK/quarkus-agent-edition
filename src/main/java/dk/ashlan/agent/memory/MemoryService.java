package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class MemoryService {
    private final SessionManager sessionManager;
    private final TaskMemoryStore memoryStore;
    private final MemoryExtractionService extractionService;

    public MemoryService(SessionManager sessionManager, TaskMemoryStore memoryStore, MemoryExtractionService extractionService) {
        this.sessionManager = sessionManager;
        this.memoryStore = memoryStore;
        this.extractionService = extractionService;
    }

    public void remember(String sessionId, String task, String message) {
        TaskMemory extracted = extractionService.extract(sessionId, task, message);
        memoryStore.save(extracted);
        sessionManager.session(sessionId).addMessage(message);
    }

    public List<String> relevantMemories(String sessionId, String query) {
        return memoryStore.findRelevant(sessionId, query, 3).stream().map(TaskMemory::memory).toList();
    }

    public List<TaskMemory> longTermMemories(String sessionId, String query, int limit) {
        return memoryStore.findRelevant(sessionId, query, limit);
    }
}
