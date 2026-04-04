package dk.ashlan.agent.core.callback;

import dk.ashlan.agent.core.AfterRunContext;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.memory.MemoryService;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@Priority(200)
public class AfterRunMemoryCallback implements AgentCallback {
    private final MemoryService memoryService;

    @Inject
    public AfterRunMemoryCallback(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public void afterRun(AfterRunContext context) {
        if (memoryService == null || context == null || context.result() == null) {
            return;
        }
        if (context.result().stopReason() == StopReason.PENDING_CONFIRMATION) {
            return;
        }
        String signal = buildSignal(context.input(), context.result().finalAnswer(), context.trace());
        if (signal == null || signal.isBlank()) {
            return;
        }
        memoryService.remember(context.sessionId(), context.input(), signal);
    }

    private String buildSignal(String input, String finalAnswer, List<String> trace) {
        StringBuilder signal = new StringBuilder(normalize(input));
        if (finalAnswer != null && !finalAnswer.isBlank()) {
            signal.append(" => ").append(normalize(finalAnswer));
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
                .map(this::truncate)
                .limit(3)
                .collect(Collectors.joining(" ; "));
    }

    private String truncate(String value) {
        String normalized = normalize(value);
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120).trim() + " …";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
