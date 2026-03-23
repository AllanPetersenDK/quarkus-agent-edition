package dk.ashlan.agent.chapters.chapter06;

import java.util.List;

public class CoreMemoryStrategyDemo {
    public List<String> run() {
        return Chapter06Support.coreMemoryStrategy().update(List.of(), "remember this");
    }
}
