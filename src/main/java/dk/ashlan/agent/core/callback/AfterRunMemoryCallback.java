package dk.ashlan.agent.core.callback;

import dk.ashlan.agent.core.AfterRunContext;
import dk.ashlan.agent.memory.MemoryService;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
        String finalAnswer = context.result().finalAnswer();
        String signal = context.input();
        if (finalAnswer != null && !finalAnswer.isBlank()) {
            signal = signal + " => " + finalAnswer;
        } else if (context.result().trace() != null && !context.result().trace().isEmpty()) {
            signal = signal + " => " + String.join(" | ", context.result().trace());
        }
        if (signal == null || signal.isBlank()) {
            return;
        }
        memoryService.remember(context.sessionId(), context.input(), signal);
    }
}
