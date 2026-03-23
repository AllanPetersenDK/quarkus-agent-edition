package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SlidingWindowStrategy implements MemoryStrategy {
    private final int windowSize = 5;

    @Override
    public String name() {
        return "sliding-window";
    }

    @Override
    public List<String> update(List<String> memory, String event) {
        ArrayList<String> updated = new ArrayList<>(memory);
        updated.add(event);
        if (updated.size() > windowSize) {
            return updated.subList(updated.size() - windowSize, updated.size());
        }
        return updated;
    }

    @Override
    public List<String> retrieve(List<String> memory, String query) {
        return memory.stream().filter(entry -> entry.contains(query)).toList();
    }
}
