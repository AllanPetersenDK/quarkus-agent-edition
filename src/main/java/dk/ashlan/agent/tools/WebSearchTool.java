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
        return new ToolDefinition(name(), "Search the live web through OpenAI Responses API for current, external, or explicitly requested lookup tasks. Not for simple stable facts or common general knowledge. Use it for current video or other up-to-date verification requests.");
    }

    @Override
    protected String executeSafely(Map<String, Object> arguments) {
        String query = String.valueOf(arguments.getOrDefault("query", ""));
        String result = webSearchBackend.search(query).toCompactText();
        if (requiresSpecificEntityAnswer(query)) {
            return result + "\nExact answer guidance: prefer the most specific named entity, species, or source explicitly supported by the search result; do not collapse to a broader category.";
        }
        return result;
    }

    private boolean requiresSpecificEntityAnswer(String query) {
        if (query == null) {
            return false;
        }
        String normalized = query.toLowerCase();
        return normalized.contains("which species")
                || normalized.contains("what species")
                || normalized.contains("which type")
                || normalized.contains("what type")
                || normalized.contains("which name")
                || normalized.contains("what name")
                || normalized.contains("which source")
                || normalized.contains("source mentions")
                || normalized.contains("who is")
                || normalized.contains("what is the name");
    }
}
