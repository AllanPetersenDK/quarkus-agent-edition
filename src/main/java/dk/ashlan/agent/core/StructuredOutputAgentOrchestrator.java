package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.StructuredOutputParser;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StructuredOutputAgentOrchestrator {
    private final StructuredOutputParser parser = new StructuredOutputParser();

    public LlmCompletion parse(String rawOutput) {
        return parser.parse(rawOutput);
    }

    public String normalize(String rawOutput) {
        LlmCompletion completion = parser.parse(rawOutput);
        return completion.content() == null ? "" : completion.content();
    }
}
