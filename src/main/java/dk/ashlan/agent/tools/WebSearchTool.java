package dk.ashlan.agent.tools;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class WebSearchTool extends AbstractTool {
    public interface WebSearchBackend {
        OpenAiWebSearchService.WebSearchResult search(String query);
    }

    private final WebSearchBackend webSearchBackend;

    @Inject
    public WebSearchTool(OpenAiWebSearchService webSearchService) {
        this((WebSearchBackend) webSearchService);
    }

    public WebSearchTool(WebSearchBackend webSearchBackend) {
        this.webSearchBackend = webSearchBackend;
    }

    @Override
    public String name() {
        return "web-search";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Search the live web through OpenAI Responses API for current, external, or explicitly requested lookup tasks. Not for simple stable facts or common general knowledge.");
    }

    @Override
    protected String executeSafely(Map<String, Object> arguments) {
        String query = String.valueOf(arguments.getOrDefault("query", ""));
        return webSearchBackend.search(query).toCompactText();
    }
}
