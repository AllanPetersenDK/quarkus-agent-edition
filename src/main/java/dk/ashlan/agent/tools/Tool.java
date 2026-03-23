package dk.ashlan.agent.tools;

import java.util.Map;

public interface Tool {
    String name();

    ToolDefinition definition();

    JsonToolResult execute(Map<String, Object> arguments);
}
