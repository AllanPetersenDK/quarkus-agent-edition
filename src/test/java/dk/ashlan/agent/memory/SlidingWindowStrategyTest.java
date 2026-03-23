package dk.ashlan.agent.memory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlidingWindowStrategyTest {
    @Test
    void keepsOnlyWindowSizedTail() {
        SlidingWindowStrategy strategy = new SlidingWindowStrategy();
        List<String> memory = List.of("1", "2", "3", "4", "5");

        List<String> updated = strategy.update(memory, "6");

        assertEquals(5, updated.size());
        assertEquals("2", updated.get(0));
        assertEquals("6", updated.get(4));
    }
}
