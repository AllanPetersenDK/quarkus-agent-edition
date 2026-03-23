package dk.ashlan.agent.chapters.chapter06;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter06SmokeTest {
    @Test
    void chapter06DemosWork() {
        assertTrue(new SessionAgentDemo().run().contains("Session"));
        assertTrue(new CoreMemoryStrategyDemo().run().contains("remember this"));
        assertTrue(new CoreMemoryUpdateDemo().run().contains("Core memory"));
        assertTrue(new SlidingWindowDemo().run().contains("Sliding"));
        assertTrue(new SummarizationDemo().run().contains("Summarization"));
        assertTrue(new ConversationSearchDemo().run().contains("Conversation"));
        assertTrue(new TaskLongTermDemo().run().contains("Task"));
        assertTrue(new UserLongTermDemo().run().contains("User"));
    }
}
