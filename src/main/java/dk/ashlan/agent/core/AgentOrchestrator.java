package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.llm.LlmClientSelector;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.memory.SessionState;
import dk.ashlan.agent.memory.SessionTraceStore;
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
    private final SessionManager sessionManager;
    private final int maxIterations;
    private final String systemPrompt;
    @Inject
    MeterRegistry meterRegistry;
    @Inject
    SessionTraceStore sessionTraceStore;

    @Inject
    public AgentOrchestrator(
            Instance<LlmClient> llmClients,
            ToolRegistry toolRegistry,
            ToolExecutor toolExecutor,
            MemoryService memoryService,
            SessionManager sessionManager,
            @ConfigProperty(name = "agent.max-iterations") int maxIterations,
            @ConfigProperty(name = "agent.system-prompt") String systemPrompt,
            Config config
    ) {
        this(selectClient(llmClients, config), toolRegistry, toolExecutor, memoryService, sessionManager, maxIterations, systemPrompt);
    }

    public AgentOrchestrator(
            LlmClient llmClient,
            ToolRegistry toolRegistry,
            ToolExecutor toolExecutor,
            MemoryService memoryService,
            int maxIterations,
            String systemPrompt
    ) {
        this(llmClient, toolRegistry, toolExecutor, memoryService, null, maxIterations, systemPrompt);
    }

    public AgentOrchestrator(
            LlmClient llmClient,
            ToolRegistry toolRegistry,
            ToolExecutor toolExecutor,
            MemoryService memoryService,
            SessionManager sessionManager,
            int maxIterations,
            String systemPrompt
    ) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.memoryService = memoryService;
        this.sessionManager = sessionManager;
        this.maxIterations = maxIterations;
        this.systemPrompt = systemPrompt;
    }

    public AgentRunResult run(String message) {
        return run(message, "default");
    }

    @WithSpan("agent.run")
    public AgentRunResult run(String message, String sessionId) {
        SessionState session = session(sessionId);
        List<LlmMessage> history = session.messages();
        ExecutionContext context = new ExecutionContext(message, sessionId, history);
        session.addUserMessage(message);
        LlmRequestBuilder requestBuilder = new LlmRequestBuilder(systemPrompt, memoryService);
        List<String> trace = new ArrayList<>();
        int iterations = 0;
        int stepNumber = nextStepNumber(sessionId);
        long startedAt = System.nanoTime();

        while (iterations < maxIterations && !context.isFinalAnswer()) {
            StepExecution stepExecution = executeStep(context, requestBuilder, session, stepNumber);
            trace.addAll(stepExecution.flatTrace());
            stepNumber++;
            if (context.isFinalAnswer()) {
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

    public AgentStepResult step(String message, String sessionId) {
        SessionState session = session(sessionId);
        List<LlmMessage> history = session.messages();
        ExecutionContext context = new ExecutionContext(message, sessionId, history);
        session.addUserMessage(message);
        LlmRequestBuilder requestBuilder = new LlmRequestBuilder(systemPrompt, memoryService);
        return executeStep(context, requestBuilder, session, nextStepNumber(sessionId)).stepResult();
    }

    private StepExecution executeStep(ExecutionContext context, LlmRequestBuilder requestBuilder, SessionState session, int stepNumber) {
        List<String> trace = new ArrayList<>();
        List<AgentTraceEntry> traceEntries = new ArrayList<>();
        List<dk.ashlan.agent.llm.LlmMessage> messages = requestBuilder.build(context);
        trace.add("iteration:" + stepNumber);
        traceEntries.add(new AgentTraceEntry("step", "iteration:" + stepNumber));

        LlmCompletion completion = llmClient.complete(messages, toolRegistry, context);
        List<LlmToolCall> toolCalls = completion.toolCalls() == null ? List.of() : List.copyOf(completion.toolCalls());
        List<JsonToolResult> toolResults = new ArrayList<>();
        if (!toolCalls.isEmpty()) {
            context.addAssistantToolCalls(toolCalls);
            session.addAssistantToolCalls(toolCalls);
            for (LlmToolCall toolCall : toolCalls) {
                JsonToolResult result = toolExecutor.execute(toolCall.toolName(), toolCall.arguments());
                toolResults.add(result);
                if (toolCall.callId() == null || toolCall.callId().isBlank()) {
                    context.addToolMessage(toolCall.toolName(), result.output());
                    session.addToolMessage(toolCall.toolName(), result.output());
                } else {
                    context.addToolMessage(toolCall.toolName(), toolCall.callId(), result.output());
                    session.addToolMessage(toolCall.toolName(), toolCall.callId(), result.output());
                }
                trace.add("tool:" + toolCall.toolName() + ":" + result.output());
                traceEntries.add(new AgentTraceEntry("tool-call", toolCall.toolName()));
                traceEntries.add(new AgentTraceEntry("tool-result", result.output()));
                if (result.success()) {
                    memoryService.remember(context.getSessionId(), context.getInput(), result.output());
                }
            }
        }

        String assistantMessage = null;
        String finalAnswer = null;
        boolean isFinal = false;
        if (completion.content() != null && !completion.content().isBlank()) {
            assistantMessage = completion.content();
            finalAnswer = completion.content();
            context.setFinalAnswer(completion.content());
            context.addAssistantMessage(completion.content());
            session.addAssistantMessage(completion.content());
            trace.add("answer:" + completion.content());
            traceEntries.add(new AgentTraceEntry("assistant-message", completion.content()));
            isFinal = true;
        }

        AgentStepResult stepResult = new AgentStepResult(
                context.getSessionId(),
                stepNumber,
                assistantMessage,
                toolCalls,
                List.copyOf(toolResults),
                finalAnswer,
                isFinal,
                List.copyOf(traceEntries)
        );
        if (sessionTraceStore != null) {
            sessionTraceStore.append(stepResult);
        }
        return new StepExecution(stepResult, List.copyOf(trace));
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

    private int nextStepNumber(String sessionId) {
        if (sessionTraceStore == null) {
            return 1;
        }
        return sessionTraceStore.load(sessionId).map(List::size).orElse(0) + 1;
    }

    private SessionState session(String sessionId) {
        if (sessionManager == null) {
            return new SessionState(sessionId);
        }
        return sessionManager.session(sessionId);
    }

    private record StepExecution(AgentStepResult stepResult, List<String> flatTrace) {
    }
}
