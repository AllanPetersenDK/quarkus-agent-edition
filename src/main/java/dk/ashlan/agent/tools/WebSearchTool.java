package dk.ashlan.agent.tools;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class WebSearchTool implements Tool {
    @Override
    public String name() {
        return "web-search";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Demo web search placeholder.");
    }

    @Override
    public JsonToolResult execute(Map<String, Object> arguments) {
        String query = String.valueOf(arguments.getOrDefault("query", ""));
        return JsonToolResult.success(name(), "Web search is a placeholder for query: " + query);
    }
}
