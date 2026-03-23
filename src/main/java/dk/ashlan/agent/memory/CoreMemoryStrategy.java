package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CoreMemoryStrategy implements MemoryStrategy {
    @Override
    public String name() {
        return "core";
    }

    @Override
    public List<String> update(List<String> memory, String event) {
        return List.of(event);
    }

    @Override
    public List<String> retrieve(List<String> memory, String query) {
        return memory.stream().filter(entry -> entry.contains(query)).toList();
    }
}
