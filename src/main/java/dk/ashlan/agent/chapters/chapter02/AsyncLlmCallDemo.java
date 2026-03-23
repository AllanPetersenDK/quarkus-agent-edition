package dk.ashlan.agent.chapters.chapter02;

import java.util.concurrent.CompletableFuture;

public class AsyncLlmCallDemo {
    public CompletableFuture<String> runAsync(String message) {
        return CompletableFuture.completedFuture("Async demo: " + message);
    }
}
