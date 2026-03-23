package dk.ashlan.agent.chapters.chapter06;

import java.util.List;

public class SlidingWindowDemo {
    public List<String> run() {
        return Chapter06Support.slidingWindowStrategy().update(List.of("1", "2", "3", "4", "5"), "6");
    }
}
