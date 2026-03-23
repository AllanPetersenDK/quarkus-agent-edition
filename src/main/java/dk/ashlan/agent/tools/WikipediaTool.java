package dk.ashlan.agent.tools;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class WikipediaTool implements Tool {
    @Override
    public String name() {
        return "wikipedia";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Demo Wikipedia search placeholder.");
    }

    @Override
    public JsonToolResult execute(Map<String, Object> arguments) {
        String query = String.valueOf(arguments.getOrDefault("query", ""));
        return JsonToolResult.success(name(), "Wikipedia placeholder for: " + query);
    }
}
