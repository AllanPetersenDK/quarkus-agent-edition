package dk.ashlan.agent.api;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@ApplicationScoped
public class CompanionAsyncBatchService {
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";
    private static final String PROVIDER_PATH = "chapter-02-async-companion";
    private static final String MAX_CONCURRENCY_CONFIG_KEY = "agent.companion.async-batch.max-concurrency";
    private static final int DEFAULT_MAX_CONCURRENCY = 4;

    private final CompanionChatCompletionsService directChatService;
    private final String defaultModel;
    private final int maxConcurrency;
    private final ExecutorService executor;

    @Inject
    public CompanionAsyncBatchService(CompanionChatCompletionsService directChatService, Config config) {
        this(
                directChatService,
                config.getOptionalValue("openai.model", String.class).orElse(DEFAULT_MODEL),
                config.getOptionalValue(MAX_CONCURRENCY_CONFIG_KEY, Integer.class).orElse(DEFAULT_MAX_CONCURRENCY)
        );
    }

    CompanionAsyncBatchService(CompanionChatCompletionsService directChatService, String defaultModel) {
        this(directChatService, defaultModel, DEFAULT_MAX_CONCURRENCY);
    }

    CompanionAsyncBatchService(CompanionChatCompletionsService directChatService, String defaultModel, int maxConcurrency) {
        this.directChatService = directChatService;
        this.defaultModel = defaultModel == null || defaultModel.isBlank() ? DEFAULT_MODEL : defaultModel.trim();
        this.maxConcurrency = Math.max(1, maxConcurrency);
        AtomicInteger threadCounter = new AtomicInteger(1);
        this.executor = Executors.newFixedThreadPool(this.maxConcurrency, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("companion-async-batch-" + threadCounter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }

    CompanionChatCompletionsResource.CompanionAsyncBatchResponse asyncBatch(
            CompanionChatCompletionsResource.CompanionAsyncBatchRequest request
    ) {
        String effectiveModel = request.model() == null || request.model().isBlank()
                ? defaultModel
                : request.model().trim();

        List<CompletableFuture<CompanionChatCompletionsResource.CompanionAsyncBatchResponse.BatchResult>> futures = IntStream.range(0, request.prompts().size())
                .mapToObj(index -> runSingleAsync(request, effectiveModel, index))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<CompanionChatCompletionsResource.CompanionAsyncBatchResponse.BatchResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        return new CompanionChatCompletionsResource.CompanionAsyncBatchResponse(
                effectiveModel,
                results,
                PROVIDER_PATH
        );
    }

    @PreDestroy
    void shutdownExecutor() {
        executor.shutdown();
    }

    private CompletableFuture<CompanionChatCompletionsResource.CompanionAsyncBatchResponse.BatchResult> runSingleAsync(
            CompanionChatCompletionsResource.CompanionAsyncBatchRequest request,
            String effectiveModel,
            int index
    ) {
        String prompt = request.prompts().get(index);
        return CompletableFuture.supplyAsync(() -> runSingle(request, effectiveModel, prompt), executor)
                .handle((result, throwable) -> throwable == null
                        ? result
                        : CompanionChatCompletionsResource.CompanionAsyncBatchResponse.BatchResult.failure(
                                prompt,
                                describeFailure(throwable)
                        ));
    }

    private CompanionChatCompletionsResource.CompanionAsyncBatchResponse.BatchResult runSingle(
            CompanionChatCompletionsResource.CompanionAsyncBatchRequest request,
            String effectiveModel,
            String prompt
    ) {
        CompanionChatCompletionsResource.CompanionChatCompletionResponse response = directChatService.complete(
                new CompanionChatCompletionsResource.CompanionChatCompletionRequest(
                        effectiveModel,
                        buildMessages(request.systemPrompt(), prompt),
                        request.temperature(),
                        null
                )
        );
        String answer = response.choices().isEmpty()
                ? ""
                : response.choices().get(0).message().content();
        return CompanionChatCompletionsResource.CompanionAsyncBatchResponse.BatchResult.success(prompt, answer);
    }

    private String describeFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current != current.getCause()) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return current.getClass().getSimpleName() + ": " + message;
    }

    private List<CompanionChatCompletionsResource.CompanionChatCompletionRequest.ChatMessage> buildMessages(
            String systemPrompt,
            String prompt
    ) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return List.of(new CompanionChatCompletionsResource.CompanionChatCompletionRequest.ChatMessage("user", prompt));
        }
        return List.of(
                new CompanionChatCompletionsResource.CompanionChatCompletionRequest.ChatMessage("system", systemPrompt),
                new CompanionChatCompletionsResource.CompanionChatCompletionRequest.ChatMessage("user", prompt)
        );
    }
}
