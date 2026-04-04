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
import dk.ashlan.agent.core.callback.AgentCallback;
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
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class AgentOrchestrator implements AgentRunner {
    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final MemoryService memoryService;
    private final SessionManager sessionManager;
    private final List<AgentCallback> callbacks;
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
            Instance<AgentCallback> callbacks,
            @ConfigProperty(name = "agent.max-iterations") int maxIterations,
            @ConfigProperty(name = "agent.system-prompt") String systemPrompt,
            Config config
    ) {
        this(selectClient(llmClients, config), toolRegistry, toolExecutor, memoryService, sessionManager, resolveCallbacks(callbacks), maxIterations, systemPrompt);
    }

    public AgentOrchestrator(
            LlmClient llmClient,
            ToolRegistry toolRegistry,
            ToolExecutor toolExecutor,
            MemoryService memoryService,
            int maxIterations,
            String systemPrompt
    ) {
        this(llmClient, toolRegistry, toolExecutor, memoryService, null, List.of(), maxIterations, systemPrompt);
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
        this(llmClient, toolRegistry, toolExecutor, memoryService, sessionManager, List.of(), maxIterations, systemPrompt);
    }

    public AgentOrchestrator(
            LlmClient llmClient,
            ToolRegistry toolRegistry,
            ToolExecutor toolExecutor,
            MemoryService memoryService,
            SessionManager sessionManager,
            List<AgentCallback> callbacks,
            int maxIterations,
            String systemPrompt
    ) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.memoryService = memoryService;
        this.sessionManager = sessionManager;
        this.callbacks = callbacks == null ? List.of() : List.copyOf(sortCallbacks(callbacks));
        this.maxIterations = maxIterations;
        this.systemPrompt = systemPrompt;
    }

    public AgentRunResult run(String message) {
        return run(message, "default");
    }

    @WithSpan("agent.run")
    public AgentRunResult run(String message, String sessionId) {
        return run(message, sessionId, List.of());
    }

    public AgentRunResult run(String message, String sessionId, List<LlmMessage> supplementalMessages) {
        SessionState session = session(sessionId);
        List<LlmMessage> history = session.messages();
        List<LlmMessage> initialMessages = new ArrayList<>(history);
        if (supplementalMessages != null && !supplementalMessages.isEmpty()) {
            initialMessages.addAll(supplementalMessages);
        }
        ExecutionContext context = new ExecutionContext(message, sessionId, initialMessages);
        session.addUserMessage(message);
        return continueRun(context, session, nextStepNumber(sessionId), List.of());
    }

    public AgentRunResult resume(String sessionId, ToolConfirmation confirmation) {
        return resume(sessionId, confirmation == null ? List.of() : List.of(confirmation));
    }

    public AgentRunResult resume(String sessionId, List<ToolConfirmation> confirmations) {
        SessionState session = session(sessionId);
        List<PendingToolCall> pendingCalls = session.pendingToolCalls();
        if (pendingCalls.isEmpty()) {
            throw new IllegalStateException("No pending tool confirmation for sessionId=" + sessionId);
        }
        ExecutionContext context = new ExecutionContext(pendingCalls.get(0).input(), sessionId, session.messages(), false);
        session.clearPendingToolCalls();

        List<String> resumeTrace = new ArrayList<>();
        java.util.Map<String, ToolConfirmation> confirmationById = new java.util.LinkedHashMap<>();
        if (confirmations != null) {
            for (ToolConfirmation candidate : confirmations) {
                if (candidate != null && candidate.toolCallId() != null && !candidate.toolCallId().isBlank()) {
                    confirmationById.put(candidate.toolCallId(), candidate);
                }
            }
        }

        for (PendingToolCall pending : pendingCalls) {
            LlmToolCall toolCall = pending.toolCall();
            String toolCallId = toolCall.callId();
            ToolConfirmation confirmation = toolCallId == null || toolCallId.isBlank() ? null : confirmationById.get(toolCallId);
            boolean approved = confirmation != null && confirmation.approved();
            String reason = confirmation == null ? "Tool call not approved." : confirmation.reason();
            JsonToolResult toolResult = approved
                    ? toolExecutor.execute(toolCall.toolName(), confirmation.arguments().isEmpty() ? toolCall.arguments() : confirmation.arguments())
                    : JsonToolResult.failure(toolCall.toolName(), reason == null || reason.isBlank() ? "Tool call not approved." : reason);
            JsonToolResult adjustedResult = fireAfterTool(context, toolCall, toolResult, pending.stepNumber());
            if (approved) {
                resumeTrace.add("pending_approved:" + toolCall.toolName() + ":" + normalizeTrace(toolCallId));
            } else {
                resumeTrace.add("pending_rejected:" + toolCall.toolName() + ":" + normalizeTrace(toolCallId) + ":" + normalizeTrace(reason));
            }
            if (toolCallId == null || toolCallId.isBlank()) {
                context.addToolMessage(toolCall.toolName(), adjustedResult.output());
                session.addToolMessage(toolCall.toolName(), adjustedResult.output());
            } else {
                context.addToolMessage(toolCall.toolName(), toolCallId, adjustedResult.output());
                session.addToolMessage(toolCall.toolName(), toolCallId, adjustedResult.output());
            }
        }
        return continueRun(context, session, pendingCalls.get(0).stepNumber() + 1, resumeTrace);
    }

    public AgentStepResult step(String message, String sessionId) {
        return step(message, sessionId, List.of());
    }

    public AgentStepResult step(String message, String sessionId, List<LlmMessage> supplementalMessages) {
        SessionState session = session(sessionId);
        List<LlmMessage> history = session.messages();
        List<LlmMessage> initialMessages = new ArrayList<>(history);
        if (supplementalMessages != null && !supplementalMessages.isEmpty()) {
            initialMessages.addAll(supplementalMessages);
        }
        ExecutionContext context = new ExecutionContext(message, sessionId, initialMessages);
        session.addUserMessage(message);
        LlmRequestBuilder requestBuilder = new LlmRequestBuilder(systemPrompt, memoryService);
        return executeStep(context, requestBuilder, session, nextStepNumber(sessionId)).stepResult();
    }

    private AgentRunResult continueRun(ExecutionContext context, SessionState session, int stepNumber, List<String> initialTrace) {
        LlmRequestBuilder requestBuilder = new LlmRequestBuilder(systemPrompt, memoryService);
        List<String> trace = new ArrayList<>();
        if (initialTrace != null && !initialTrace.isEmpty()) {
            trace.addAll(initialTrace);
        }
        int iterations = 0;
        long startedAt = System.nanoTime();
        int currentStep = stepNumber;

        while (iterations < maxIterations && !context.isFinalAnswer()) {
            StepExecution stepExecution = executeStep(context, requestBuilder, session, currentStep);
            trace.addAll(stepExecution.flatTrace());
            if (!stepExecution.pendingToolCalls().isEmpty()) {
                for (PendingToolCall pendingToolCall : stepExecution.pendingToolCalls()) {
                    trace.add("pending_confirmation:" + pendingToolCall.toolCall().toolName());
                }
                AgentRunResult result = new AgentRunResult("", StopReason.PENDING_CONFIRMATION, Math.min(iterations + 1, maxIterations), trace, List.copyOf(stepExecution.pendingToolCalls()));
                recordAgentRunMetrics(StopReason.PENDING_CONFIRMATION, iterations, System.nanoTime() - startedAt);
                fireAfterRun(context, result);
                return result;
            }
            currentStep++;
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
        AgentRunResult result = new AgentRunResult(context.getFinalAnswer(), stopReason, Math.min(iterations + 1, maxIterations), trace, List.of());
        fireAfterRun(context, result);
        return result;
    }

    private String normalizeTrace(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private StepExecution executeStep(ExecutionContext context, LlmRequestBuilder requestBuilder, SessionState session, int stepNumber) {
        List<String> trace = new ArrayList<>();
        List<AgentTraceEntry> traceEntries = new ArrayList<>();
        List<dk.ashlan.agent.llm.LlmMessage> messages = requestBuilder.build(context);
        trace.add("iteration:" + stepNumber);
        traceEntries.add(new AgentTraceEntry("step", "iteration:" + stepNumber));

        BeforeLlmContext beforeLlmContext = fireBeforeLlm(context, messages, stepNumber);
        List<dk.ashlan.agent.llm.LlmMessage> optimizedMessages = beforeLlmContext.projectedMessages()
                .orElse(beforeLlmContext.messages());
        beforeLlmContext.optimizationSummary().ifPresent(summary -> {
            trace.add("context:" + summary);
            traceEntries.add(new AgentTraceEntry("context", summary));
        });
        LlmCompletion completion = llmClient.complete(optimizedMessages, toolRegistry, context);
        fireAfterLlm(context, optimizedMessages, completion, stepNumber);
        List<LlmToolCall> toolCalls = completion.toolCalls() == null ? List.of() : List.copyOf(completion.toolCalls());
        List<JsonToolResult> toolResults = new ArrayList<>();
        List<PendingToolCall> pendingToolCalls = new ArrayList<>();
        if (!toolCalls.isEmpty()) {
            context.addAssistantToolCalls(toolCalls);
            session.addAssistantToolCalls(toolCalls);
            for (LlmToolCall toolCall : toolCalls) {
                dk.ashlan.agent.tools.Tool tool = toolRegistry.find(toolCall.toolName());
                if (tool != null && tool.definition() != null && tool.definition().requiresConfirmation()) {
                    PendingToolCall pendingToolCall = new PendingToolCall(
                            context.getSessionId(),
                            stepNumber,
                            context.getInput(),
                            toolCall,
                            tool.definition().confirmationMessageTemplate()
                    );
                    session.addPendingToolCall(pendingToolCall);
                    trace.add("pending_confirmation:" + toolCall.toolName());
                    trace.add("pending_confirmation_id:" + normalizeTrace(toolCall.callId()));
                    traceEntries.add(new AgentTraceEntry("pending-confirmation", toolCall.toolName()));
                    pendingToolCalls.add(pendingToolCall);
                    continue;
                }
                if (!fireBeforeTool(context, toolCall, stepNumber)) {
                    JsonToolResult blocked = JsonToolResult.failure(toolCall.toolName(), "Tool blocked by callback: " + toolCall.toolName());
                    JsonToolResult finalBlocked = fireAfterTool(context, toolCall, blocked, stepNumber);
                    toolResults.add(finalBlocked);
                    if (toolCall.callId() == null || toolCall.callId().isBlank()) {
                        context.addToolMessage(toolCall.toolName(), finalBlocked.output());
                        session.addToolMessage(toolCall.toolName(), finalBlocked.output());
                    } else {
                        context.addToolMessage(toolCall.toolName(), toolCall.callId(), finalBlocked.output());
                        session.addToolMessage(toolCall.toolName(), toolCall.callId(), finalBlocked.output());
                    }
                    trace.add("tool:" + toolCall.toolName() + ":" + finalBlocked.output());
                    traceEntries.add(new AgentTraceEntry("tool-call", toolCall.toolName()));
                    traceEntries.add(new AgentTraceEntry("tool-result", finalBlocked.output()));
                    continue;
                }
                JsonToolResult result = toolExecutor.execute(toolCall.toolName(), toolCall.arguments());
                JsonToolResult adjustedResult = fireAfterTool(context, toolCall, result, stepNumber);
                toolResults.add(adjustedResult);
                if (toolCall.callId() == null || toolCall.callId().isBlank()) {
                    context.addToolMessage(toolCall.toolName(), adjustedResult.output());
                    session.addToolMessage(toolCall.toolName(), adjustedResult.output());
                } else {
                    context.addToolMessage(toolCall.toolName(), toolCall.callId(), adjustedResult.output());
                    session.addToolMessage(toolCall.toolName(), toolCall.callId(), adjustedResult.output());
                }
                trace.add("tool:" + toolCall.toolName() + ":" + adjustedResult.output());
                traceEntries.add(new AgentTraceEntry("tool-call", toolCall.toolName()));
                traceEntries.add(new AgentTraceEntry("tool-result", adjustedResult.output()));
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
        if (!pendingToolCalls.isEmpty()) {
            assistantMessage = null;
            finalAnswer = null;
            isFinal = false;
            context.setFinalAnswer("");
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
        return new StepExecution(stepResult, List.copyOf(trace), List.copyOf(pendingToolCalls));
    }

    private BeforeLlmContext fireBeforeLlm(ExecutionContext context, List<LlmMessage> messages, int stepNumber) {
        BeforeLlmContext callbackContext = new BeforeLlmContext(context.getSessionId(), stepNumber, List.copyOf(messages));
        if (callbacks.isEmpty()) {
            return callbackContext;
        }
        for (AgentCallback callback : callbacks) {
            callback.beforeLlm(callbackContext);
        }
        return callbackContext;
    }

    private void fireAfterLlm(ExecutionContext context, List<LlmMessage> messages, LlmCompletion completion, int stepNumber) {
        if (callbacks.isEmpty()) {
            return;
        }
        AfterLlmContext callbackContext = new AfterLlmContext(context.getSessionId(), stepNumber, List.copyOf(messages), completion);
        for (AgentCallback callback : callbacks) {
            callback.afterLlm(callbackContext);
        }
    }

    private boolean fireBeforeTool(ExecutionContext context, LlmToolCall toolCall, int stepNumber) {
        if (callbacks.isEmpty()) {
            return true;
        }
        BeforeToolContext callbackContext = new BeforeToolContext(context.getSessionId(), stepNumber, toolCall);
        for (AgentCallback callback : callbacks) {
            if (!callback.beforeTool(callbackContext)) {
                return false;
            }
        }
        return true;
    }

    private JsonToolResult fireAfterTool(ExecutionContext context, LlmToolCall toolCall, JsonToolResult toolResult, int stepNumber) {
        if (callbacks.isEmpty()) {
            return toolResult;
        }
        JsonToolResult current = toolResult;
        for (AgentCallback callback : callbacks) {
            JsonToolResult adjusted = callback.afterTool(new AfterToolContext(context.getSessionId(), stepNumber, toolCall, current));
            if (adjusted != null) {
                current = adjusted;
            }
        }
        return current;
    }

    private void fireAfterRun(ExecutionContext context, AgentRunResult result) {
        if (callbacks.isEmpty()) {
            return;
        }
        AfterRunContext callbackContext = new AfterRunContext(context.getSessionId(), context.getInput(), result, result.trace());
        for (AgentCallback callback : callbacks) {
            callback.afterRun(callbackContext);
        }
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

    private static List<AgentCallback> resolveCallbacks(Instance<AgentCallback> callbacks) {
        if (callbacks == null) {
            return List.of();
        }
        List<AgentCallback> resolved = new ArrayList<>();
        for (AgentCallback callback : callbacks) {
            resolved.add(callback);
        }
        return sortCallbacks(resolved);
    }

    private static List<AgentCallback> sortCallbacks(List<AgentCallback> callbacks) {
        return callbacks.stream()
                .sorted(Comparator.comparingInt(AgentOrchestrator::priorityOf)
                        .thenComparing(callback -> callback.getClass().getName()))
                .toList();
    }

    private static int priorityOf(AgentCallback callback) {
        jakarta.annotation.Priority priority = callback.getClass().getAnnotation(jakarta.annotation.Priority.class);
        return priority == null ? 0 : priority.value();
    }

    private record StepExecution(AgentStepResult stepResult, List<String> flatTrace, List<PendingToolCall> pendingToolCalls) {
    }
}
