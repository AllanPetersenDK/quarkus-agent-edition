package dk.ashlan.agent.api;

import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmClientSelector;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.tools.ToolRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@ApplicationScoped
public class CompanionChatCompletionsService {
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";
    private static final String PROVIDER_PATH = "chapter-02-companion-debug";
    private final LlmClient llmClient;
    private final String defaultModel;
    private final ToolRegistry emptyToolRegistry = new ToolRegistry(List.of());

    @Inject
    public CompanionChatCompletionsService(
            Instance<LlmClient> llmClients,
            Config config,
            @ConfigProperty(name = "openai.api-key", defaultValue = "") String openAiApiKey
    ) {
        this(selectClient(llmClients, config, openAiApiKey), config.getOptionalValue("openai.model", String.class).orElse(DEFAULT_MODEL));
    }

    CompanionChatCompletionsService(LlmClient llmClient, String defaultModel) {
        this.llmClient = llmClient;
        this.defaultModel = defaultModel == null || defaultModel.isBlank() ? DEFAULT_MODEL : defaultModel.trim();
    }

    public CompanionChatCompletionsResource.CompanionChatCompletionResponse complete(
            CompanionChatCompletionsResource.CompanionChatCompletionRequest request
    ) {
        String effectiveModel = request.model() == null || request.model().isBlank()
                ? defaultModel
                : request.model().trim();
        List<LlmMessage> messages = request.messages().stream()
                .map(message -> new LlmMessage(normalizeRole(message.role()), message.content(), null, null, List.of()))
                .toList();
        String contextInput = latestUserMessage(request.messages());
        ExecutionContext context = new ExecutionContext(contextInput, "companion-llm-completions");
        LlmCompletion completion = llmClient.complete(messages, emptyToolRegistry, context);
        String assistantContent = completion.content();
        if (assistantContent == null || assistantContent.isBlank()) {
            assistantContent = summarizeToolCalls(completion.toolCalls());
        }
        if (assistantContent == null || assistantContent.isBlank()) {
            assistantContent = "No assistant content was returned by the selected LLM client.";
        }
        return new CompanionChatCompletionsResource.CompanionChatCompletionResponse(
                effectiveModel,
                List.of(new CompanionChatCompletionsResource.CompanionChatCompletionResponse.Choice(
                        0,
                        new CompanionChatCompletionsResource.CompanionChatCompletionResponse.Message("assistant", assistantContent)
                )),
                PROVIDER_PATH
        );
    }

    private static LlmClient selectClient(Instance<LlmClient> llmClients, Config config, String openAiApiKey) {
        String requestedProvider = config.getOptionalValue("agent.llm-provider", String.class).orElse("auto");
        return LlmClientSelector.select(llmClients, requestedProvider, openAiApiKey);
    }

    private String latestUserMessage(List<CompanionChatCompletionsResource.CompanionChatCompletionRequest.ChatMessage> messages) {
        String latestUser = messages.stream()
                .filter(message -> "user".equalsIgnoreCase(message.role()))
                .map(CompanionChatCompletionsResource.CompanionChatCompletionRequest.ChatMessage::content)
                .reduce((first, second) -> second)
                .orElse("");
        if (!latestUser.isBlank()) {
            return latestUser;
        }
        return messages.isEmpty() ? "" : messages.get(messages.size() - 1).content();
    }

    private String summarizeToolCalls(List<LlmToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return "";
        }
        String toolNames = toolCalls.stream()
                .map(LlmToolCall::toolName)
                .distinct()
                .collect(Collectors.joining(", "));
        return "Tool calls were requested but not executed by this companion debug seam: " + toolNames;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "user";
        }
        return switch (role.toLowerCase(Locale.ROOT)) {
            case "system", "assistant", "tool", "user" -> role.toLowerCase(Locale.ROOT);
            default -> "user";
        };
    }
}
