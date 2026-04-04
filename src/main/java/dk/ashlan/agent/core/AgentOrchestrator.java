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
import dk.ashlan.agent.core.callback.AfterRunMemoryCallback;
import dk.ashlan.agent.planning.ExecutionPlan;
import dk.ashlan.agent.planning.PlanStep;
import dk.ashlan.agent.planning.Chapter7ReflectionState;
import dk.ashlan.agent.tools.JsonToolResult;
import dk.ashlan.agent.tools.ProcessLlmRequestTool;
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
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class AgentOrchestrator implements AgentRunner {
    private static final Pattern CONFIRMATION_FILE_PATTERN = Pattern.compile("(?i)\\b([\\w./-]+\\.[\\w\\d]+)\\b");
    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final MemoryService memoryService;
    private final SessionManager sessionManager;
    private final List<AgentCallback> callbacks;
    private final ProcessLlmRequestTool processLlmRequestTool;
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
            ProcessLlmRequestTool processLlmRequestTool,
            @ConfigProperty(name = "agent.max-iterations") int maxIterations,
            @ConfigProperty(name = "agent.system-prompt") String systemPrompt,
            Config config
    ) {
        this(selectClient(llmClients, config), toolRegistry, toolExecutor, memoryService, sessionManager, resolveCallbacks(callbacks), processLlmRequestTool, maxIterations, systemPrompt);
    }

    public AgentOrchestrator(
            LlmClient llmClient,
            ToolRegistry toolRegistry,
            ToolExecutor toolExecutor,
            MemoryService memoryService,
            int maxIterations,
            String systemPrompt
    ) {
        this(llmClient, toolRegistry, toolExecutor, memoryService, null, List.of(), null, maxIterations, systemPrompt);
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
        this(llmClient, toolRegistry, toolExecutor, memoryService, sessionManager, List.of(), null, maxIterations, systemPrompt);
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
        this(llmClient, toolRegistry, toolExecutor, memoryService, sessionManager, callbacks, null, maxIterations, systemPrompt);
    }

    public AgentOrchestrator(
            LlmClient llmClient,
            ToolRegistry toolRegistry,
            ToolExecutor toolExecutor,
            MemoryService memoryService,
            SessionManager sessionManager,
            List<AgentCallback> callbacks,
            ProcessLlmRequestTool processLlmRequestTool,
            int maxIterations,
            String systemPrompt
    ) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.memoryService = memoryService;
        this.sessionManager = sessionManager;
        this.callbacks = callbacks == null ? List.of() : List.copyOf(sortCallbacks(callbacks));
        this.processLlmRequestTool = processLlmRequestTool;
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
        context.putAttribute("skipDeterministicConfirmationPreflight", Boolean.TRUE);
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
        LlmRequestBuilder requestBuilder = new LlmRequestBuilder(systemPrompt, memoryService, processLlmRequestTool);
        return executeStep(context, requestBuilder, session, nextStepNumber(sessionId)).stepResult();
    }

    private AgentRunResult continueRun(ExecutionContext context, SessionState session, int stepNumber, List<String> initialTrace) {
        LlmRequestBuilder requestBuilder = new LlmRequestBuilder(systemPrompt, memoryService, processLlmRequestTool);
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
        Object requestPrepTraceEntry = context.getAttribute(LlmRequestBuilder.REQUEST_PREP_TRACE_ATTRIBUTE);
        if (requestPrepTraceEntry instanceof AgentTraceEntry entry) {
            trace.add(entry.kind() + ":" + entry.message());
            traceEntries.add(entry);
        }
        trace.add("iteration:" + stepNumber);
        traceEntries.add(new AgentTraceEntry("step", "iteration:" + stepNumber));

        Optional<StepExecution> deterministicPending = deterministicConfirmationStep(context, session, stepNumber, trace, traceEntries);
        if (deterministicPending.isPresent()) {
            return deterministicPending.get();
        }

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
                    recordChapter7Trace(trace, traceEntries, toolCall, finalBlocked);
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
                recordChapter7Trace(trace, traceEntries, toolCall, adjustedResult);
                recordChapter7State(session, toolCall, adjustedResult);
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

    private Optional<StepExecution> deterministicConfirmationStep(
            ExecutionContext context,
            SessionState session,
            int stepNumber,
            List<String> trace,
            List<AgentTraceEntry> traceEntries
    ) {
        if (Boolean.TRUE.equals(context.getAttribute("skipDeterministicConfirmationPreflight"))) {
            return Optional.empty();
        }
        dk.ashlan.agent.tools.Tool deleteTool = toolRegistry == null ? null : toolRegistry.find("delete-file");
        if (deleteTool == null || deleteTool.definition() == null || !deleteTool.definition().requiresConfirmation()) {
            return Optional.empty();
        }

        String input = normalizeTrace(context.getInput());
        if (!looksLikeDeleteRequest(input)) {
            return Optional.empty();
        }

        String path = extractConfirmationPath(input);
        if (path.isBlank()) {
            return Optional.empty();
        }

        String callId = deterministicCallId(context.getSessionId(), stepNumber, deleteTool.name(), path);
        LlmToolCall toolCall = new LlmToolCall(deleteTool.name(), java.util.Map.of("path", path), callId);
        PendingToolCall pendingToolCall = new PendingToolCall(
                context.getSessionId(),
                stepNumber,
                context.getInput(),
                toolCall,
                confirmationMessage(deleteTool.definition().confirmationMessageTemplate(), path)
        );

        context.addAssistantToolCalls(List.of(toolCall));
        session.addAssistantToolCalls(List.of(toolCall));
        session.addPendingToolCall(pendingToolCall);

        trace.add("pending_confirmation:" + deleteTool.name());
        trace.add("pending_confirmation_id:" + normalizeTrace(callId));
        trace.add("pending_confirmation_message:" + normalizeTrace(pendingToolCall.confirmationMessage()));
        traceEntries.add(new AgentTraceEntry("pending-confirmation", deleteTool.name()));
        traceEntries.add(new AgentTraceEntry("pending-confirmation-id", normalizeTrace(callId)));

        AgentStepResult stepResult = new AgentStepResult(
                context.getSessionId(),
                stepNumber,
                null,
                List.of(toolCall),
                List.of(),
                null,
                false,
                List.copyOf(traceEntries)
        );
        if (sessionTraceStore != null) {
            sessionTraceStore.append(stepResult);
        }
        return Optional.of(new StepExecution(stepResult, List.copyOf(trace), List.of(pendingToolCall)));
    }

    private boolean looksLikeDeleteRequest(String input) {
        String normalized = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return normalized.contains("delete")
                || normalized.contains("remove")
                || normalized.contains("slet")
                || normalized.contains("fjern");
    }

    private String extractConfirmationPath(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        Matcher matcher = CONFIRMATION_FILE_PATTERN.matcher(input);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate != null && !candidate.isBlank()) {
                return candidate.replaceAll("^['\"`]|['\"`]$", "");
            }
        }
        return "";
    }

    private String confirmationMessage(String template, String path) {
        String base = template == null || template.isBlank()
                ? "Approve deleting the requested workspace file?"
                : template;
        return base + " path=" + path;
    }

    private String deterministicCallId(String sessionId, int stepNumber, String toolName, String path) {
        int hash = Math.abs(Objects.hash(sessionId, stepNumber, toolName, path));
        return "call_preflight_" + Integer.toHexString(hash);
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
        if (memoryService != null
                && context != null
                && result != null
                && result.stopReason() != StopReason.PENDING_CONFIRMATION
                && !hasAfterRunMemoryCallback()) {
            memoryService.remember(context.getSessionId(), context.getInput(), buildAfterRunMemorySignal(context.getInput(), result.finalAnswer(), result.trace()));
        }
        if (callbacks.isEmpty()) {
            return;
        }
        AfterRunContext callbackContext = new AfterRunContext(context.getSessionId(), context.getInput(), result, result.trace());
        for (AgentCallback callback : callbacks) {
            callback.afterRun(callbackContext);
        }
    }

    private void recordChapter7Trace(List<String> trace, List<AgentTraceEntry> traceEntries, LlmToolCall toolCall, JsonToolResult toolResult) {
        if (toolCall == null || toolResult == null || toolCall.toolName() == null) {
            return;
        }
        String toolName = toolCall.toolName();
        String output = toolResult.output() == null ? "" : toolResult.output();
        if ("create-tasks".equals(toolName)) {
            trace.add("chapter7-plan:" + normalizeTrace(output));
            traceEntries.add(new AgentTraceEntry("planning", output));
        } else if ("reflection".equals(toolName)) {
            trace.add("chapter7-reflection:" + normalizeTrace(output));
            traceEntries.add(new AgentTraceEntry("reflection", output));
            if (output.contains("REPLAN NEEDED")) {
                trace.add("chapter7-replan:" + normalizeTrace(output));
                traceEntries.add(new AgentTraceEntry("replan", "needed"));
            }
        }
    }

    private void recordChapter7State(SessionState session, LlmToolCall toolCall, JsonToolResult toolResult) {
        if (session == null || toolCall == null || toolCall.toolName() == null) {
            return;
        }
        if ("create-tasks".equals(toolCall.toolName())) {
            session.setChapter7Plan(chapter7Plan(toolCall));
            return;
        }
        if ("reflection".equals(toolCall.toolName())) {
            session.setChapter7Reflection(chapter7Reflection(toolCall, toolResult));
        }
    }

    @SuppressWarnings("unchecked")
    private ExecutionPlan chapter7Plan(LlmToolCall toolCall) {
        java.util.Map<String, Object> arguments = toolCall.arguments();
        String goal = stringArgument(arguments, "goal");
        Object tasksValue = arguments == null ? null : arguments.get("tasks");
        List<PlanStep> steps = new ArrayList<>();
        if (tasksValue instanceof List<?> list) {
            int order = 1;
            for (Object item : list) {
                if (item instanceof java.util.Map<?, ?> rawMap) {
                    steps.add(new PlanStep(
                            order++,
                            stringArgument((java.util.Map<String, Object>) rawMap, "content"),
                            statusArgument((java.util.Map<String, Object>) rawMap, "status"),
                            stringArgument((java.util.Map<String, Object>) rawMap, "doneWhen"),
                            stringArgument((java.util.Map<String, Object>) rawMap, "notes")
                    ));
                }
            }
        }
        return new ExecutionPlan(goal, steps);
    }

    private Chapter7ReflectionState chapter7Reflection(LlmToolCall toolCall, JsonToolResult toolResult) {
        java.util.Map<String, Object> arguments = toolCall.arguments();
        String summary = toolResult == null ? "" : toolResult.output();
        boolean needReplan = booleanArgument(arguments, "needReplan");
        return new Chapter7ReflectionState(
                stringArgument(arguments, "mode"),
                stringArgument(arguments, "analysis"),
                !needReplan,
                needReplan,
                booleanArgument(arguments, "readyToAnswer"),
                stringArgument(arguments, "alternativeDirection"),
                stringArgument(arguments, "nextStep"),
                summary
        );
    }

    private String stringArgument(java.util.Map<String, Object> arguments, String key) {
        if (arguments == null) {
            return "";
        }
        Object value = arguments.get(key);
        return value == null ? "" : value.toString();
    }

    private boolean booleanArgument(java.util.Map<String, Object> arguments, String key) {
        if (arguments == null) {
            return false;
        }
        Object value = arguments.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private dk.ashlan.agent.planning.TaskStatus statusArgument(java.util.Map<String, Object> arguments, String key) {
        String value = stringArgument(arguments, key).toLowerCase(Locale.ROOT);
        return switch (value) {
            case "completed" -> dk.ashlan.agent.planning.TaskStatus.COMPLETED;
            case "in_progress", "in-progress", "active" -> dk.ashlan.agent.planning.TaskStatus.IN_PROGRESS;
            default -> dk.ashlan.agent.planning.TaskStatus.PENDING;
        };
    }

    private String buildAfterRunMemorySignal(String input, String finalAnswer, List<String> trace) {
        StringBuilder signal = new StringBuilder(normalizeTrace(input));
        if (finalAnswer != null && !finalAnswer.isBlank()) {
            signal.append(" => ").append(normalizeTrace(finalAnswer));
        }
        String tracePreview = compactTrace(trace);
        if (!tracePreview.isBlank()) {
            signal.append(" | trace: ").append(tracePreview);
        }
        return signal.toString().trim();
    }

    private String compactTrace(List<String> trace) {
        if (trace == null || trace.isEmpty()) {
            return "";
        }
        return trace.stream()
                .filter(entry -> entry != null && !entry.isBlank())
                .map(this::truncateTraceEntry)
                .limit(3)
                .collect(java.util.stream.Collectors.joining(" ; "));
    }

    private String truncateTraceEntry(String value) {
        String normalized = normalizeTrace(value);
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120).trim() + " …";
    }

    private boolean hasAfterRunMemoryCallback() {
        return callbacks.stream().anyMatch(callback -> callback instanceof AfterRunMemoryCallback);
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
