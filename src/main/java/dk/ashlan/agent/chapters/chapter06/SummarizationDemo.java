package dk.ashlan.agent.chapters.chapter06;

import java.util.List;

public class SummarizationDemo {
    public List<String> run() {
        return Chapter06Support.summarizationStrategy().update(List.of("hello"), "world");
    }
}
