package dk.ashlan.agent.chapters.chapter03;

import dk.ashlan.agent.tools.WebSearchTool;

public class TavilySearchToolDemo {
    private final WebSearchTool webSearchTool = new WebSearchTool();

    public String run(String query) {
        return webSearchTool.execute(java.util.Map.of("query", query)).output();
    }
}
