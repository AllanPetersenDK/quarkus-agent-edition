package dk.ashlan.agent.tools;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class WikipediaTool extends AbstractTool {
    @Override
    public String name() {
        return "wikipedia";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Search Wikipedia when the user explicitly asks for Wikipedia or needs a sourced lookup. Not for simple stable facts.");
    }

    @Override
    protected String executeSafely(Map<String, Object> arguments) {
        String query = String.valueOf(arguments.getOrDefault("query", ""));
        return "Wikipedia placeholder for: " + query;
    }
}
