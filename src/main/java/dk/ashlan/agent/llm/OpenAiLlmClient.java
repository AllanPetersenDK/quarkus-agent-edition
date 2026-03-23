package dk.ashlan.agent.llm;

import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.tools.ToolRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@ApplicationScoped
@Alternative
public class OpenAiLlmClient implements LlmClient {
    private final String apiKey;
    private final String model;

    public OpenAiLlmClient(
            @ConfigProperty(name = "openai.api-key", defaultValue = "") String apiKey,
            @ConfigProperty(name = "openai.model", defaultValue = "gpt-4.1-mini") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public LlmCompletion complete(List<LlmMessage> messages, ToolRegistry toolRegistry, ExecutionContext context) {
        if (apiKey == null || apiKey.isBlank()) {
            return LlmCompletion.answer("OpenAI client placeholder for model " + model + ". Configure openai.api-key to enable it.");
        }
        return LlmCompletion.answer("OpenAI client is configured but this companion edition keeps the implementation as a placeholder.");
    }
}
