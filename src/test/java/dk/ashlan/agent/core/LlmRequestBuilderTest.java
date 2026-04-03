package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmRequestBuilderTest {
    @Test
    void buildPreparesTheRuntimePromptToAnswerStableFactsDirectly() {
        LlmRequestBuilder builder = new LlmRequestBuilder(
                "You are a helpful Quarkus agent. Answer simple, timeless, widely known facts directly without tools. Use tools only when the answer depends on current information, external verification, session memory, or the user explicitly asks you to look something up. Prefer the most specific tool and do not use knowledge-base, wikipedia, or web-search for basic general knowledge. Examples: What is the capital of France? -> answer directly. What time is it right now? -> use clock. Search Wikipedia for the capital of France. -> use wikipedia because the user explicitly requested lookup.",
                null
        );

        List<LlmMessage> messages = builder.build(new ExecutionContext("What is the capital of France?"));

        assertEquals("system", messages.get(0).role());
        assertTrue(messages.get(0).content().contains("Answer simple, timeless, widely known facts directly without tools."));
        assertTrue(messages.get(0).content().contains("What is the capital of France? -> answer directly."));
        assertTrue(messages.get(0).content().contains("What time is it right now? -> use clock."));
        assertTrue(messages.get(0).content().contains("Search Wikipedia for the capital of France. -> use wikipedia because the user explicitly requested lookup."));
    }
}
