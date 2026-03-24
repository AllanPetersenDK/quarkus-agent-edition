package dk.ashlan.agent.chapters.chapter06;

import dk.ashlan.agent.memory.CoreMemoryStrategy;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.InMemorySessionStateStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.memory.SlidingWindowStrategy;
import dk.ashlan.agent.memory.SummarizationStrategy;
import dk.ashlan.agent.sessions.InMemorySessionManager;
import dk.ashlan.agent.sessions.TaskCrossSessionManager;
import dk.ashlan.agent.sessions.UserCrossSessionManager;

import java.util.List;

final class Chapter06Support {
    private Chapter06Support() {
    }

    static InMemorySessionManager sessions() {
        return new InMemorySessionManager();
    }

    static SessionManager memorySessions() {
        return new SessionManager(new InMemorySessionStateStore());
    }

    static MemoryService memoryService() {
        return new MemoryService(memorySessions(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());
    }

    static CoreMemoryStrategy coreMemoryStrategy() {
        return new CoreMemoryStrategy();
    }

    static SlidingWindowStrategy slidingWindowStrategy() {
        return new SlidingWindowStrategy();
    }

    static SummarizationStrategy summarizationStrategy() {
        return new SummarizationStrategy();
    }

    static TaskCrossSessionManager taskCrossSessionManager() {
        return new TaskCrossSessionManager();
    }

    static UserCrossSessionManager userCrossSessionManager() {
        return new UserCrossSessionManager();
    }

    static List<String> seedMemory(String sessionId, String task, String... messages) {
        MemoryService memoryService = memoryService();
        for (String message : messages) {
            memoryService.remember(sessionId, task, message);
        }
        return memoryService.relevantMemories(sessionId, task);
    }
}
