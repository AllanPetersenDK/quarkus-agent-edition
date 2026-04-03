package dk.ashlan.agent.api;

import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionAsyncBatchServiceTest {
    @Test
    void asyncBatchBoundsConcurrencyAndPreservesOrder() throws Exception {
        AtomicInteger activeCalls = new AtomicInteger();
        AtomicInteger maxObservedConcurrency = new AtomicInteger();
        CountDownLatch firstTwoStarted = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        LlmClient client = (messages, toolRegistry, context) -> {
            String prompt = extractPrompt(messages);
            int current = activeCalls.incrementAndGet();
            maxObservedConcurrency.accumulateAndGet(current, Math::max);
            firstTwoStarted.countDown();
            try {
                if (!release.await(2, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting for the batch test to release");
                }
                return LlmCompletion.answer("answer:" + prompt);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            } finally {
                activeCalls.decrementAndGet();
            }
        };
        CompanionChatCompletionsService directChatService = new CompanionChatCompletionsService(client, "gpt-4.1-mini");
        CompanionAsyncBatchService service = new CompanionAsyncBatchService(directChatService, "gpt-4.1-mini", 2);

        CompletableFuture<CompanionChatCompletionsResource.CompanionAsyncBatchResponse> responseFuture =
                CompletableFuture.supplyAsync(() -> service.asyncBatch(
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
                ));

        assertTrue(firstTwoStarted.await(2, TimeUnit.SECONDS));
        assertEquals(2, maxObservedConcurrency.get());

        release.countDown();
        CompanionChatCompletionsResource.CompanionAsyncBatchResponse response = responseFuture.get(2, TimeUnit.SECONDS);

        assertEquals("gpt-4.1-mini", response.model());
        assertEquals("chapter-02-async-companion", response.providerPath());
        assertEquals(3, response.results().size());
        assertEquals("What is 2 + 2?", response.results().get(0).prompt());
        assertEquals("answer:What is 2 + 2?", response.results().get(0).answer());
        assertNull(response.results().get(0).error());
        assertEquals("What is the capital of Japan?", response.results().get(1).prompt());
        assertEquals("answer:What is the capital of Japan?", response.results().get(1).answer());
        assertNull(response.results().get(1).error());
        assertEquals("Who wrote Romeo and Juliet?", response.results().get(2).prompt());
        assertEquals("answer:Who wrote Romeo and Juliet?", response.results().get(2).answer());
        assertNull(response.results().get(2).error());
    }

    @Test
    void asyncBatchSurfacesOnePromptFailureWithoutFailingTheBatch() {
        LlmClient client = (messages, toolRegistry, context) -> {
            String prompt = extractPrompt(messages);
            if ("Who wrote Romeo and Juliet?".equals(prompt)) {
                throw new IllegalStateException("boom");
            }
            return LlmCompletion.answer("answer:" + prompt);
        };
        CompanionChatCompletionsService directChatService = new CompanionChatCompletionsService(client, "gpt-4.1-mini");
        CompanionAsyncBatchService service = new CompanionAsyncBatchService(directChatService, "gpt-4.1-mini", 2);

        CompanionChatCompletionsResource.CompanionAsyncBatchResponse response = service.asyncBatch(
                new CompanionChatCompletionsResource.CompanionAsyncBatchRequest(
                        null,
                        List.of(
                                "What is 2 + 2?",
                                "Who wrote Romeo and Juliet?",
                                "What is the capital of Japan?"
                        ),
                        "You are a helpful assistant.",
                        0.0
                )
        );

        assertEquals(3, response.results().size());
        assertEquals("What is 2 + 2?", response.results().get(0).prompt());
        assertEquals("answer:What is 2 + 2?", response.results().get(0).answer());
        assertNull(response.results().get(0).error());
        assertEquals("Who wrote Romeo and Juliet?", response.results().get(1).prompt());
        assertNull(response.results().get(1).answer());
        assertFalse(response.results().get(1).error().isBlank());
        assertTrue(response.results().get(1).error().contains("boom"));
        assertEquals("What is the capital of Japan?", response.results().get(2).prompt());
        assertEquals("answer:What is the capital of Japan?", response.results().get(2).answer());
        assertNull(response.results().get(2).error());
    }

    private String extractPrompt(List<LlmMessage> messages) {
        return messages.stream()
                .filter(message -> "user".equals(message.role()))
                .map(LlmMessage::content)
                .reduce((first, second) -> second)
                .orElse("");
    }
}
