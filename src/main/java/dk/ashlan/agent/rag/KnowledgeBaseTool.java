package dk.ashlan.agent.rag;

import dk.ashlan.agent.tools.JsonToolResult;
import dk.ashlan.agent.tools.Tool;
import dk.ashlan.agent.tools.ToolDefinition;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class KnowledgeBaseTool implements Tool {
    private final RagService ragService;

    public KnowledgeBaseTool(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public String name() {
        return "knowledge-base";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Search the in-memory knowledge base.");
    }

    @Override
    public JsonToolResult execute(Map<String, Object> arguments) {
        String query = String.valueOf(arguments.getOrDefault("query", ""));
        int topK = Integer.parseInt(String.valueOf(arguments.getOrDefault("topK", 3)));
        return JsonToolResult.success(name(), ragService.answer(query, topK));
    }
}
