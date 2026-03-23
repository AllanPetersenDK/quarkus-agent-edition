package dk.ashlan.agent.tools;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class WebSearchTool extends AbstractTool {
    @Override
    public String name() {
        return "web-search";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Demo web search placeholder.");
    }

    @Override
    protected String executeSafely(Map<String, Object> arguments) {
        String query = String.valueOf(arguments.getOrDefault("query", ""));
        return "Web search is a placeholder for query: " + query;
    }
}
