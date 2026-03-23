package dk.ashlan.agent.chapters.chapter06;

import dk.ashlan.agent.memory.MemoryService;

import java.util.List;

public class ConversationSearchDemo {
    public List<String> run() {
        MemoryService memoryService = Chapter06Support.memoryService();
        memoryService.remember("chapter-06", "conversation search", "My name is Ada");
        memoryService.remember("chapter-06", "conversation search", "I live in Copenhagen");
        memoryService.remember("chapter-06", "conversation search", "I like Quarkus");
        return memoryService.relevantMemories("chapter-06", "Quarkus");
    }
}
