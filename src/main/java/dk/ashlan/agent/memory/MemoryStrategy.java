package dk.ashlan.agent.memory;

import java.util.List;

public interface MemoryStrategy {
    String name();

    List<String> update(List<String> memory, String event);

    List<String> retrieve(List<String> memory, String query);
}
