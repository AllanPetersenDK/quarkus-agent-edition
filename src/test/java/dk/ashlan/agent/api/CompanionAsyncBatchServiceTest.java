package dk.ashlan.agent.api;

import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanionAsyncBatchServiceTest {
    @Test
    void asyncBatchReturnsOneOrderedResultPerPrompt() {
        LlmClient client = (messages, toolRegistry, context) -> {
            String prompt = messages.stream()
                    .filter(message -> "user".equals(message.role()))
                    .map(LlmMessage::content)
                    .reduce((first, second) -> second)
                    .orElse("");
            if ("What is 2 + 2?".equals(prompt)) {
                sleep(80);
                return LlmCompletion.answer("2 + 2 = 4");
            }
            if ("What is the capital of Japan?".equals(prompt)) {
                sleep(10);
                return LlmCompletion.answer("The capital of Japan is Tokyo.");
            }
            sleep(40);
            return LlmCompletion.answer("William Shakespeare wrote Romeo and Juliet.");
        };
        CompanionChatCompletionsService directChatService = new CompanionChatCompletionsService(client, "gpt-4.1-mini");
        CompanionAsyncBatchService service = new CompanionAsyncBatchService(directChatService, "gpt-4.1-mini");

        CompanionChatCompletionsResource.CompanionAsyncBatchResponse response = service.asyncBatch(
                new CompanionChatCompletionsResource.CompanionAsyncBatchRequest(
                        null,
                        List.of(
                                "What is 2 + 2?",
                                "What is the capital of Japan?",
                                "Who wrote Romeo and Juliet?"
                        ),
                        "You are a helpful assistant.",
                        0.0
                )
        );

        assertEquals("gpt-4.1-mini", response.model());
        assertEquals("chapter-02-async-companion", response.providerPath());
        assertEquals(3, response.results().size());
        assertEquals("What is 2 + 2?", response.results().get(0).prompt());
        assertEquals("2 + 2 = 4", response.results().get(0).answer());
        assertEquals("What is the capital of Japan?", response.results().get(1).prompt());
        assertEquals("The capital of Japan is Tokyo.", response.results().get(1).answer());
        assertEquals("Who wrote Romeo and Juliet?", response.results().get(2).prompt());
        assertEquals("William Shakespeare wrote Romeo and Juliet.", response.results().get(2).answer());
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
