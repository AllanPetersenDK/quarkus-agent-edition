package dk.ashlan.agent.chapters.chapter03;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter03FlowTest {
    @Test
    void toolRegistryAndDecoratorFlowWorks() {
        assertTrue(Chapter03Support.registry().definitions().containsKey("calculator"));
        assertTrue(Chapter03Support.executor().execute("calculator", java.util.Map.of("expression", "25 * 4")).output().contains("100"));
        assertTrue(Chapter03Support.decoratedCalculator().execute(java.util.Map.of("expression", "25 * 4")).output().contains("[decorated]"));
        assertTrue(Chapter03Support.echoTool().execute(java.util.Map.of("value", "x")).output().contains("value"));
    }
}
