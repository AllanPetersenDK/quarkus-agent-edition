package dk.ashlan.agent.tools;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class ToolExecutor {
    private final ToolRegistry toolRegistry;

    public ToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public JsonToolResult execute(String toolName, Map<String, Object> arguments) {
        Tool tool = toolRegistry.find(toolName);
        if (tool == null) {
            return JsonToolResult.failure(toolName, "Unknown tool: " + toolName);
        }
        return tool.execute(arguments);
    }
}
