package dk.ashlan.agent.chapters.chapter06;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter06SmokeTest {
    @Test
    void chapter06DemosWork() {
        assertTrue(listContains(new SessionAgentDemo().run(), "hello"));
        assertTrue(listContains(new CoreMemoryStrategyDemo().run(), "remember this"));
        assertTrue(listContains(new CoreMemoryUpdateDemo().run(), "and this"));
        assertTrue(listContains(new SlidingWindowDemo().run(), "6"));
        assertTrue(listContains(new SummarizationDemo().run(), "world"));
        assertTrue(listContains(new ConversationSearchDemo().run(), "Quarkus"));
        assertTrue(listContains(new TaskLongTermDemo().run(), "quarkus"));
        assertTrue(listContains(new UserLongTermDemo().run(), "java"));
    }

    private boolean listContains(List<String> values, String fragment) {
        return values.stream().anyMatch(value -> value.contains(fragment));
    }
}
