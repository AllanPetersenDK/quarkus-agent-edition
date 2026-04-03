package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.llm.LlmClientSelector;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.tools.JsonToolResult;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class AgentOrchestrator implements AgentRunner {
    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final MemoryService memoryService;
    private final int maxIterations;
    private final String systemPrompt;
    @Inject
    MeterRegistry meterRegistry;

    @Inject
    public AgentOrchestrator(
            Instance<LlmClient> llmClients,
            ToolRegistry toolRegistry,
            ToolExecutor toolExecutor,
            MemoryService memoryService,
            @ConfigProperty(name = "agent.max-iterations") int maxIterations,
            @ConfigProperty(name = "agent.system-prompt") String systemPrompt,
            Config config
    ) {
        this(selectClient(llmClients, config), toolRegistry, toolExecutor, memoryService, maxIterations, systemPrompt);
    }

    public AgentOrchestrator(
            LlmClient llmClient,
            ToolRegistry toolRegistry,
            ToolExecutor toolExecutor,
            MemoryService memoryService,
            int maxIterations,
            String systemPrompt
    ) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.memoryService = memoryService;
        this.maxIterations = maxIterations;
        this.systemPrompt = systemPrompt;
    }

    public AgentRunResult run(String message) {
        return run(message, "default");
    }

    @WithSpan("agent.run")
    public AgentRunResult run(String message, String sessionId) {
        ExecutionContext context = new ExecutionContext(message, sessionId);
        LlmRequestBuilder requestBuilder = new LlmRequestBuilder(systemPrompt, memoryService);
        List<String> trace = new ArrayList<>();
        int iterations = 0;
        long startedAt = System.nanoTime();

        while (iterations < maxIterations && !context.isFinalAnswer()) {
            trace.add("iteration:" + (iterations + 1));
            List<dk.ashlan.agent.llm.LlmMessage> messages = requestBuilder.build(context);
            LlmCompletion completion = llmClient.complete(messages, toolRegistry, context);
            if (!completion.toolCalls().isEmpty()) {
                context.addAssistantToolCalls(completion.toolCalls());
                for (LlmToolCall toolCall : completion.toolCalls()) {
                    JsonToolResult result = toolExecutor.execute(toolCall.toolName(), toolCall.arguments());
                    if (toolCall.callId() == null || toolCall.callId().isBlank()) {
                        context.addToolMessage(toolCall.toolName(), result.output());
                    } else {
                        context.addToolMessage(toolCall.toolName(), toolCall.callId(), result.output());
                    }
                    trace.add("tool:" + toolCall.toolName() + ":" + result.output());
                    if (result.success()) {
                        memoryService.remember(sessionId, message, result.output());
                    }
                }
            }
            if (completion.content() != null && !completion.content().isBlank()) {
                context.setFinalAnswer(completion.content());
                context.addAssistantMessage(completion.content());
                trace.add("answer:" + completion.content());
                break;
            }
            iterations++;
        }

        if (!context.isFinalAnswer()) {
            context.setFinalAnswer("");
        }
        StopReason stopReason = context.isFinalAnswer() ? StopReason.FINAL_ANSWER : StopReason.MAX_ITERATIONS;
        recordAgentRunMetrics(stopReason, iterations, System.nanoTime() - startedAt);
        return new AgentRunResult(context.getFinalAnswer(), stopReason, Math.min(iterations + 1, maxIterations), trace);
    }

    private void recordAgentRunMetrics(StopReason stopReason, int iterations, long elapsedNanos) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.timer("agent.run.duration", "stopReason", stopReason.name())
                .record(elapsedNanos, TimeUnit.NANOSECONDS);
        meterRegistry.counter("agent.run.total", "stopReason", stopReason.name()).increment();
        meterRegistry.counter("agent.run.iterations", "stopReason", stopReason.name())
                .increment(Math.max(0, iterations));
    }

    private static LlmClient selectClient(Instance<LlmClient> llmClients, Config config) {
        String openAiApiKey = config.getOptionalValue("openai.api-key", String.class).orElse("");
        String requestedProvider = config.getOptionalValue("agent.llm-provider", String.class).orElse("auto");
        return LlmClientSelector.select(llmClients, requestedProvider, openAiApiKey);
    }
}
