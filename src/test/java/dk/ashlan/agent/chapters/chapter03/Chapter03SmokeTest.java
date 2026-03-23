package dk.ashlan.agent.chapters.chapter03;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter03SmokeTest {
    @Test
    void chapter03DemosWork() {
        assertTrue(new CalculatorToolDemo().run().contains("100"));
        assertTrue(new TavilySearchToolDemo().run("quarkus").contains("quarkus"));
        assertTrue(new WikipediaToolDemo().run("java").contains("java"));
        assertTrue(new ToolDefinitionDemo().run().contains("arithmetic"));
        assertTrue(new ToolSchemaDemo().run().contains("calculator"));
        assertTrue(new ToolsExerciseDemo().run().contains("100"));
        assertTrue(new ToolAbstractionDemo().run().name().contains("abstract"));
        assertTrue(new ToolDecoratorDemo().run("25 * 4").contains("[decorated]"));
        assertTrue(new ToolDecoratorWrapperDemo().run().contains("[wrapped]"));
        assertTrue(new McpTavilyCustomDemo().run("x").contains("MCP"));
    }
}
