package dk.ashlan.agent.core.callback;

import dk.ashlan.agent.core.BeforeLlmContext;
import dk.ashlan.agent.core.ContextOptimizationResult;
import dk.ashlan.agent.core.ContextOptimizer;
import dk.ashlan.agent.llm.LlmRequest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Priority(25)
public class ContextOptimizationCallback implements AgentCallback {
    private final ContextOptimizer optimizer;

    @Inject
    public ContextOptimizationCallback(ContextOptimizer optimizer) {
        this.optimizer = optimizer;
    }

    @Override
    public void beforeLlm(BeforeLlmContext context) {
        if (context == null) {
            return;
        }
        ContextOptimizationResult result = optimizer.optimize(new LlmRequest(context.messages()));
        if (!result.changed()) {
            context.setOptimizationSummary("context-optimizer:none:cache-friendly:" + result.originalTokenCount());
            return;
        }
        context.projectMessages(result.messages());
        context.setOptimizationSummary("context-optimizer:" + result.strategy()
                + ":cache-rewrite"
                + ":" + result.originalTokenCount()
                + "->" + result.projectedTokenCount());
    }
}
