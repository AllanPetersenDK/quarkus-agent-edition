package dk.ashlan.agent.chapters.chapter02;

import dk.ashlan.agent.llm.LlmResponse;

import java.util.concurrent.CompletableFuture;

public class AsyncLlmCallDemo {
    public CompletableFuture<LlmResponse> runAsync(String message) {
        return CompletableFuture.supplyAsync(() -> LlmResponse.answer("Async demo: " + message));
    }
}
