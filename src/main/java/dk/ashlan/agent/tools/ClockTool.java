package dk.ashlan.agent.tools;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.Map;

@ApplicationScoped
public class ClockTool implements Tool {
    @Override
    public String name() {
        return "clock";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Return the current time in ISO-8601 format.");
    }

    @Override
    public JsonToolResult execute(Map<String, Object> arguments) {
        return JsonToolResult.success(name(), OffsetDateTime.now().toString());
    }

    public String getTime() {
        return OffsetDateTime.now().toString();
    }
}
