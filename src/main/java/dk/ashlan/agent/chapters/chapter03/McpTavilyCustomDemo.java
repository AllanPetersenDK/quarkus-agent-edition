package dk.ashlan.agent.chapters.chapter03;

public class McpTavilyCustomDemo {
    public String run(String query) {
        return "MCP Tavily demo placeholder for: " + query + " via " + Chapter03Support.executor().execute("web-search", java.util.Map.of("query", query)).output();
    }
}
