package dk.ashlan.agent.chapters.chapter06;

import java.util.List;

public class CoreMemoryUpdateDemo {
    public List<String> run() {
        return Chapter06Support.coreMemoryStrategy().update(List.of("remember this"), "and this");
    }
}
