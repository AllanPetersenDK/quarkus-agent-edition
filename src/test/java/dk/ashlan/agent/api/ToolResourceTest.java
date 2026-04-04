package dk.ashlan.agent.api;

import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ToolRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolResourceTest {
    @Test
    void listToolsReturnsRegisteredToolDefinitions() {
        ToolResource resource = new ToolResource(new ToolRegistry(List.of(new CalculatorTool())));

        var tools = resource.listTools();

        assertEquals(1, tools.size());
        assertEquals("calculator", tools.get(0).name());
        assertEquals("Evaluate a simple arithmetic expression.", tools.get(0).description());
    }

    @Test
    void toolResourceIsApplicationScopedForRuntimeDiscovery() {
        assertTrue(ToolResource.class.isAnnotationPresent(ApplicationScoped.class));
    }
}
