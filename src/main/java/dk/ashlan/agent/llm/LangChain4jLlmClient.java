package dk.ashlan.agent.llm;

import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.tools.ToolRegistry;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class LangChain4jLlmClient implements BaseLlmClient {
    private final LangChain4jCompanionAssistant assistant;

    public LangChain4jLlmClient(LangChain4jCompanionAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public LlmCompletion complete(List<LlmMessage> messages, ToolRegistry toolRegistry, ExecutionContext context) {
        String prompt = buildPrompt(messages, toolRegistry, context);
        String answer = assistant.answer(prompt);
        if (answer == null || answer.isBlank()) {
            return LlmCompletion.answer("LangChain4j companion seam returned no answer.");
        }
        return LlmCompletion.answer(answer.trim());
    }

    private String buildPrompt(List<LlmMessage> messages, ToolRegistry toolRegistry, ExecutionContext context) {
        String latestUserMessage = messages.stream()
                .filter(message -> "user".equals(message.role()))
                .map(LlmMessage::content)
                .reduce((first, second) -> second)
                .orElse(context.getInput());
        String availableTools = toolRegistry.tools().stream()
                .map(tool -> tool.name())
                .collect(Collectors.joining(", "));
        return """
                Comparison seam: LangChain4j-backed LLM adapter.

                Session: %s
                Latest user message: %s
                Available manual tools in the repo: %s
                """.formatted(context.getSessionId(), latestUserMessage, availableTools);
    }
}
