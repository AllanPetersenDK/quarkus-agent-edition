package dk.ashlan.agent.llm;

import java.util.Locale;

public final class LlmClientSelector {
    private LlmClientSelector() {
    }

    public static LlmClient select(Iterable<LlmClient> clients, String provider, String openAiApiKey) {
        String normalizedProvider = provider == null || provider.isBlank()
                ? "auto"
                : provider.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedProvider) {
            case "openai" -> find(clients, OpenAiLlmClient.class);
            case "langchain4j", "framework" -> find(clients, LangChain4jLlmClient.class);
            case "demo" -> find(clients, DemoToolCallingLlmClient.class);
            default -> selectAuto(clients, openAiApiKey);
        };
    }

    private static LlmClient selectAuto(Iterable<LlmClient> clients, String openAiApiKey) {
        if (openAiApiKey != null && !openAiApiKey.isBlank()) {
            LlmClient openAi = findOrNull(clients, OpenAiLlmClient.class);
            if (openAi != null) {
                return openAi;
            }
        }
        LlmClient demo = findOrNull(clients, DemoToolCallingLlmClient.class);
        if (demo != null) {
            return demo;
        }
        LlmClient langChain4j = findOrNull(clients, LangChain4jLlmClient.class);
        if (langChain4j != null) {
            return langChain4j;
        }
        return first(clients);
    }

    private static LlmClient find(Iterable<LlmClient> clients, Class<? extends LlmClient> type) {
        LlmClient client = findOrNull(clients, type);
        if (client == null) {
            throw new IllegalStateException("No LLM client available for " + type.getSimpleName());
        }
        return client;
    }

    private static LlmClient findOrNull(Iterable<LlmClient> clients, Class<? extends LlmClient> type) {
        for (LlmClient client : clients) {
            if (type.isInstance(client)) {
                return client;
            }
        }
        return null;
    }

    private static LlmClient first(Iterable<LlmClient> clients) {
        for (LlmClient client : clients) {
            return client;
        }
        throw new IllegalStateException("No LLM client available");
    }
}
