package dk.ashlan.agent.tools;

import jakarta.enterprise.context.ApplicationScoped;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ToolExecutor {
    private final ToolRegistry toolRegistry;
    @Inject
    MeterRegistry meterRegistry;

    public ToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @WithSpan("tool.execute")
    public JsonToolResult execute(String toolName, Map<String, Object> arguments) {
        long startedAt = System.nanoTime();
        Tool tool = toolRegistry.find(toolName);
        JsonToolResult result;
        if (tool == null) {
            result = JsonToolResult.failure(toolName, "Unknown tool: " + toolName);
        } else {
            result = tool.execute(arguments);
        }
        recordToolMetrics(toolName, result.success(), System.nanoTime() - startedAt);
        return result;
    }

    private void recordToolMetrics(String toolName, boolean success, long elapsedNanos) {
        if (meterRegistry == null) {
            return;
        }
        String successTag = String.valueOf(success);
        meterRegistry.timer("agent.tool.execute.duration", "tool", toolName, "success", successTag)
                .record(elapsedNanos, TimeUnit.NANOSECONDS);
        meterRegistry.counter("agent.tool.execute.total", "tool", toolName, "success", successTag)
                .increment();
    }
}
