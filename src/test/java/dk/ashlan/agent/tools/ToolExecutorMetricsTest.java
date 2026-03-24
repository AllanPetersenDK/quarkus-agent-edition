package dk.ashlan.agent.tools;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolExecutorMetricsTest {
    @Test
    void recordsMetricsForSuccessfulAndFailedToolCalls() {
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new CalculatorTool()));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        toolExecutor.meterRegistry = meterRegistry;

        JsonToolResult success = toolExecutor.execute("calculator", Map.of("expression", "25 * 4"));
        JsonToolResult failure = toolExecutor.execute("missing", Map.of());

        assertEquals(1.0, meterRegistry.counter("agent.tool.execute.total", "tool", "calculator", "success", "true").count());
        assertEquals(1.0, meterRegistry.counter("agent.tool.execute.total", "tool", "missing", "success", "false").count());
        assertEquals(1L, meterRegistry.find("agent.tool.execute.duration").tags("tool", "calculator", "success", "true").timer().count());
        assertEquals(1L, meterRegistry.find("agent.tool.execute.duration").tags("tool", "missing", "success", "false").timer().count());
        assertEquals(true, success.success());
        assertEquals(false, failure.success());
    }
}
