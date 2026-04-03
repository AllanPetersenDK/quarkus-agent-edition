package dk.ashlan.agent.llm;

public record LlmModelConfig(String provider, String model, String baseUrl) {
    public static LlmModelConfig openAi(String model) {
        return new LlmModelConfig("openai", model, "https://api.openai.com/v1");
    }

    public static LlmModelConfig langChain4j(String model) {
        return new LlmModelConfig("langchain4j", model, "https://api.openai.com/v1");
    }
}
