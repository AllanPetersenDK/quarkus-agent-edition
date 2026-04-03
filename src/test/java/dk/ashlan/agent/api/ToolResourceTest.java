package dk.ashlan.agent.api;

import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolResourceTest {
    @Test
    void listToolsReturnsRegisteredToolDefinitions() {
        ToolResource resource = new ToolResource(new ToolRegistry(List.of(new CalculatorTool())));

        var tools = resource.listTools();

        assertEquals(1, tools.size());
        assertEquals("calculator", tools.get(0).name());
        assertEquals("Evaluate a simple arithmetic expression.", tools.get(0).description());
    }
}
