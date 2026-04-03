package dk.ashlan.agent.api;

import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionChatCompletionsServiceTest {
    @Test
    void completeReturnsAnOpenAiStyleAssistantChoice() {
        AtomicReference<List<LlmMessage>> capturedMessages = new AtomicReference<>();
        LlmClient client = (messages, toolRegistry, context) -> {
            capturedMessages.set(messages);
            assertTrue(toolRegistry.tools().isEmpty());
            assertEquals("What is the capital of France?", context.getInput());
            return LlmCompletion.answer("The capital of France is Paris.");
        };
        CompanionChatCompletionsService service = new CompanionChatCompletionsService(client, "gpt-4.1-mini");

        CompanionChatCompletionsResource.CompanionChatCompletionResponse response = service.complete(
                new CompanionChatCompletionsResource.CompanionChatCompletionRequest(
                        null,
                        List.of(
                                new CompanionChatCompletionsResource.CompanionChatCompletionRequest.ChatMessage("system", "You are a helpful assistant."),
                                new CompanionChatCompletionsResource.CompanionChatCompletionRequest.ChatMessage("user", "What is the capital of France?")
                        ),
                        0.0,
                        32
                )
        );

        assertEquals("gpt-4.1-mini", response.model());
        assertEquals("chapter-02-companion-debug", response.providerPath());
        assertEquals(1, response.choices().size());
        assertEquals(0, response.choices().get(0).index());
        assertEquals("assistant", response.choices().get(0).message().role());
        assertEquals("The capital of France is Paris.", response.choices().get(0).message().content());
        assertEquals(2, capturedMessages.get().size());
        assertEquals("system", capturedMessages.get().get(0).role());
        assertEquals("user", capturedMessages.get().get(1).role());
    }

    @Test
    void completeSummarizesToolCallsWithoutExecutingThem() {
        LlmClient client = (messages, toolRegistry, context) -> LlmCompletion.toolCalls(
                List.of(new LlmToolCall("calculator", Map.of("expression", "25 * 4")))
        );
        CompanionChatCompletionsService service = new CompanionChatCompletionsService(client, "gpt-4.1-mini");

        CompanionChatCompletionsResource.CompanionChatCompletionResponse response = service.complete(
                new CompanionChatCompletionsResource.CompanionChatCompletionRequest(
                        "gpt-4.1-mini",
                        List.of(new CompanionChatCompletionsResource.CompanionChatCompletionRequest.ChatMessage("user", "What is 25 * 4?")),
                        null,
                        null
                )
        );

        assertTrue(response.choices().get(0).message().content().contains("Tool calls were requested"));
        assertTrue(response.choices().get(0).message().content().contains("calculator"));
    }
}
