package dk.ashlan.agent.chapters.chapter06;

import dk.ashlan.agent.memory.CoreMemoryStrategy;

import java.util.List;

public class CoreMemoryStrategyDemo {
    public List<String> run() {
        return new CoreMemoryStrategy().update(List.of(), "remember this");
    }
}
