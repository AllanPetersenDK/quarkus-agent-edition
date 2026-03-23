package dk.ashlan.agent.chapters.chapter06;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter06FlowTest {
    @Test
    void sessionContinuityAndCrossSessionMemoryWork() {
        var sessionManager = Chapter06Support.sessions();
        var session = sessionManager.getOrCreate("chapter-06");
        session.addEvent("first");
        session.addEvent("second");

        assertEquals(2, sessionManager.getOrCreate("chapter-06").events().size());

        var memoryService = Chapter06Support.memoryService();
        memoryService.remember("chapter-06", "profile", "My name is Ada");
        memoryService.remember("chapter-06", "profile", "I live in Copenhagen");

        assertTrue(memoryService.longTermMemories("chapter-06", "Ada", 10).stream()
                .anyMatch(memory -> memory.memory().contains("Ada")));
    }
}
