package dk.ashlan.agent.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmModelConfigTest {
    @Test
    void openAiConfigUsesExpectedDefaults() {
        LlmModelConfig config = LlmModelConfig.openAi("gpt-4.1-mini");

        assertEquals("openai", config.provider());
        assertEquals("gpt-4.1-mini", config.model());
    }
}
