package dk.ashlan.agent.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@ApplicationScoped
public class CompanionAsyncBatchService {
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";
    private static final String PROVIDER_PATH = "chapter-02-async-companion";

    private final CompanionChatCompletionsService directChatService;
    private final String defaultModel;

    @Inject
    public CompanionAsyncBatchService(CompanionChatCompletionsService directChatService, Config config) {
        this(directChatService, config.getOptionalValue("openai.model", String.class).orElse(DEFAULT_MODEL));
    }

    CompanionAsyncBatchService(CompanionChatCompletionsService directChatService, String defaultModel) {
        this.directChatService = directChatService;
        this.defaultModel = defaultModel == null || defaultModel.isBlank() ? DEFAULT_MODEL : defaultModel.trim();
    }

    CompanionChatCompletionsResource.CompanionAsyncBatchResponse asyncBatch(
            CompanionChatCompletionsResource.CompanionAsyncBatchRequest request
    ) {
        String effectiveModel = request.model() == null || request.model().isBlank()
                ? defaultModel
                : request.model().trim();

        List<CompletableFuture<CompanionChatCompletionsResource.CompanionAsyncBatchResponse.BatchResult>> futures = IntStream.range(0, request.prompts().size())
                .mapToObj(index -> CompletableFuture.supplyAsync(() -> runSingle(request, index)))
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

    private CompanionChatCompletionsResource.CompanionAsyncBatchResponse.BatchResult runSingle(
            CompanionChatCompletionsResource.CompanionAsyncBatchRequest request,
            int index
    ) {
        String prompt = request.prompts().get(index);
        CompanionChatCompletionsResource.CompanionChatCompletionResponse response = directChatService.complete(
                new CompanionChatCompletionsResource.CompanionChatCompletionRequest(
                        request.model(),
                        buildMessages(request.systemPrompt(), prompt),
                        request.temperature(),
                        null
                )
        );
        String answer = response.choices().isEmpty()
                ? ""
                : response.choices().get(0).message().content();
        return new CompanionChatCompletionsResource.CompanionAsyncBatchResponse.BatchResult(prompt, answer);
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
