package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SummarizationStrategy implements MemoryStrategy {
    @Override
    public String name() {
        return "summarization";
    }

    @Override
    public List<String> update(List<String> memory, String event) {
        if (memory.isEmpty()) {
            return List.of(event);
        }
        return List.of(memory.get(0) + " | " + event);
    }

    @Override
    public List<String> retrieve(List<String> memory, String query) {
        return memory.stream().filter(entry -> entry.contains(query)).toList();
    }
}
