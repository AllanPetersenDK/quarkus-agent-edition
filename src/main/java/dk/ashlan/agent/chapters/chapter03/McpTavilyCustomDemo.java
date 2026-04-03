package dk.ashlan.agent.chapters.chapter03;

import java.util.Map;

public class McpTavilyCustomDemo {
    public String run(String query) {
        return "MCP Tavily demo placeholder for: " + query + " via " + Chapter03Support.webSearchTool().execute(Map.of("query", query)).output();
    }
}
