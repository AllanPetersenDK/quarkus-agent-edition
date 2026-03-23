package dk.ashlan.agent.llm;

public record LlmModelConfig(String provider, String model, String baseUrl) {
    public static LlmModelConfig openAi(String model) {
        return new LlmModelConfig("openai", model, "https://api.openai.com/v1");
    }
}
