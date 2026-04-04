package dk.ashlan.agent.core;

import dk.ashlan.agent.core.callback.ContextOptimizationCallback;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmRequest;
import dk.ashlan.agent.memory.SlidingWindowStrategy;
import dk.ashlan.agent.memory.SummarizationStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextOptimizerTest {
    @Test
    void doesNothingWhenUnderThreshold() {
        ContextOptimizer optimizer = new ContextOptimizer(
                new SlidingWindowStrategy(),
                new SummarizationStrategy(),
                80,
                4,
                3,
                40
        );

        List<LlmMessage> messages = List.of(
                LlmMessage.system("You are helpful."),
                LlmMessage.user("Hello"),
                LlmMessage.assistant("Hi there")
        );

        ContextOptimizationResult result = optimizer.optimize(new LlmRequest(messages));

        assertFalse(result.changed());
        assertEquals(messages, result.messages());
    }

    @Test
    void compactionReducesLargeToolPayloadsBeforeSummarization() {
        ContextOptimizer optimizer = new ContextOptimizer(
                new SlidingWindowStrategy(),
                new SummarizationStrategy(),
                60,
                4,
                3,
                40
        );
        List<LlmMessage> messages = List.of(
                LlmMessage.system("You are helpful."),
                LlmMessage.user("Explain the report"),
                LlmMessage.tool("web-search", "call-1", "result ".repeat(120))
        );

        ContextOptimizationResult result = optimizer.optimize(new LlmRequest(messages));

        assertEquals("compaction", result.strategy());
        assertTrue(result.projectedTokenCount() < result.originalTokenCount());
        assertTrue(result.messages().get(2).content().contains("compacted tool output"));
    }

    @Test
    void summarizationKeepsTheUserFramingAndRecentTail() {
        ContextOptimizer optimizer = new ContextOptimizer(
                new SlidingWindowStrategy(),
                new SummarizationStrategy(),
                20,
                4,
                2,
                10
        );
        List<LlmMessage> messages = List.of(
                LlmMessage.system("You are helpful."),
                LlmMessage.user("Problem framing"),
                LlmMessage.assistant("Acknowledged"),
                LlmMessage.tool("calculator", "call-1", "100"),
                LlmMessage.user("What is the answer?"),
                LlmMessage.assistant("The answer is 100"),
                LlmMessage.tool("memory", "Remember the answer")
        );

        ContextOptimizationResult result = optimizer.optimize(new LlmRequest(messages));

        assertEquals("summarization", result.strategy());
        assertTrue(result.messages().stream().anyMatch(message ->
                "system".equals(message.role()) && message.content().startsWith("Context summary: ")
        ));
        assertTrue(result.messages().stream().anyMatch(message ->
                "user".equals(message.role()) && "What is the answer?".equals(message.content())
        ));
    }

    @Test
    void tokenCountRoughlyTracksPayloadSize() {
        LlmRequest request = new LlmRequest(List.of(
                LlmMessage.user("one two three"),
                LlmMessage.assistant("four five six")
        ));

        assertTrue(request.estimatedTokenCount() > 0);
    }

    @Test
    void beforeLlmCallbackCanProjectOptimizedMessages() {
        ContextOptimizationCallback callback = new ContextOptimizationCallback(
                new ContextOptimizer(
                        new SlidingWindowStrategy(),
                        new SummarizationStrategy(),
                        30,
                        4,
                        3,
                        20
                )
        );
        BeforeLlmContext context = new BeforeLlmContext(
                "session-1",
                1,
                List.of(
                        LlmMessage.system("You are helpful."),
                        LlmMessage.user("Explain the report"),
                        LlmMessage.tool("web-search", "call-1", "result ".repeat(50))
                )
        );

        callback.beforeLlm(context);

        assertTrue(context.projectedMessages().isPresent());
        assertTrue(context.optimizationSummary().orElse("").startsWith("context-optimizer:"));
    }
}
