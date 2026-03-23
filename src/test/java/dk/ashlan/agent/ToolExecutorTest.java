package dk.ashlan.agent;

import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ClockTool;
import dk.ashlan.agent.tools.JsonToolResult;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolExecutorTest {
    @Test
    void calculatorToolIsExecutedThroughRegistry() {
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new CalculatorTool(), new ClockTool()));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);

        JsonToolResult result = toolExecutor.execute("calculator", Map.of("expression", "25 * 4"));

        assertTrue(result.success());
        assertTrue(result.output().contains("100"));
    }

    @Test
    void unknownToolFailsCleanly() {
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new CalculatorTool()));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);

        JsonToolResult result = toolExecutor.execute("missing", Map.of());

        assertEquals(false, result.success());
        assertTrue(result.output().contains("Unknown tool"));
    }
}
