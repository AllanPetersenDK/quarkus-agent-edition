package dk.ashlan.agent.memory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SummarizationStrategyTest {
    @Test
    void summarizesIntoSingleEntry() {
        SummarizationStrategy strategy = new SummarizationStrategy();

        List<String> updated = strategy.update(List.of("hello"), "world");

        assertTrue(updated.get(0).contains("hello"));
        assertTrue(updated.get(0).contains("world"));
    }
}
