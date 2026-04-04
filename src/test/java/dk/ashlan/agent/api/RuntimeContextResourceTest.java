package dk.ashlan.agent.api;

import dk.ashlan.agent.api.dto.ContextOptimizeRequest;
import dk.ashlan.agent.api.dto.ContextOptimizeResponse;
import dk.ashlan.agent.core.ContextOptimizer;
import dk.ashlan.agent.memory.SlidingWindowStrategy;
import dk.ashlan.agent.memory.SummarizationStrategy;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.Path;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeContextResourceTest {
    @Test
    void resourceExposesChapterSixContextOptimizationSeam() throws Exception {
        Tag tag = RuntimeContextResource.class.getAnnotation(Tag.class);
        Path classPath = RuntimeContextResource.class.getAnnotation(Path.class);
        Method optimize = RuntimeContextResource.class.getMethod("optimize", ContextOptimizeRequest.class);
        Operation operation = optimize.getAnnotation(Operation.class);
        Path methodPath = optimize.getAnnotation(Path.class);

        assertNotNull(tag);
        assertNotNull(classPath);
        assertNotNull(methodPath);
        assertNotNull(operation);
        assertEquals("/api/runtime/context", classPath.value());
        assertEquals("/optimize", methodPath.value());
        assertTrue(tag.description().contains("chapter-6"));
        assertTrue(operation.description().contains("request-time context optimization inspection seam"));
    }

    @Test
    void optimizeReturnsOriginalProjectedAndStrategyDetails() {
        RuntimeContextResource resource = new RuntimeContextResource(
                new ContextOptimizer(
                        new SlidingWindowStrategy(),
                        new SummarizationStrategy(),
                        60,
                        4,
                        2,
                        40
                )
        );

        ContextOptimizeResponse unchanged = resource.optimize(new ContextOptimizeRequest(List.of(
                new ContextOptimizeRequest.ContextOptimizeMessage("system", "You are helpful.", null, null),
                new ContextOptimizeRequest.ContextOptimizeMessage("user", "Hello", null, null)
        )));
        assertEquals("none", unchanged.strategy());
        assertTrue(!unchanged.changed());
        assertTrue(unchanged.originalTokenCount() > 0);
        assertEquals(unchanged.originalTokenCount(), unchanged.projectedTokenCount());

        ContextOptimizeResponse compaction = resource.optimize(new ContextOptimizeRequest(List.of(
                new ContextOptimizeRequest.ContextOptimizeMessage("system", "You are helpful.", null, null),
                new ContextOptimizeRequest.ContextOptimizeMessage("tool", "result ".repeat(80), "web-search", "call-1")
        )));
        assertEquals("compaction", compaction.strategy());
        assertTrue(compaction.changed());
        assertTrue(compaction.projectedTokenCount() < compaction.originalTokenCount());

        RuntimeContextResource summarizationResource = new RuntimeContextResource(
                new ContextOptimizer(
                        new SlidingWindowStrategy(),
                        new SummarizationStrategy(),
                        20,
                        4,
                        2,
                        10
                )
        );
        ContextOptimizeResponse summarization = summarizationResource.optimize(new ContextOptimizeRequest(List.of(
                new ContextOptimizeRequest.ContextOptimizeMessage("system", "You are helpful.", null, null),
                new ContextOptimizeRequest.ContextOptimizeMessage("user", "Problem framing", null, null),
                new ContextOptimizeRequest.ContextOptimizeMessage("assistant", "Acknowledged", null, null),
                new ContextOptimizeRequest.ContextOptimizeMessage("tool", "100", "calculator", "call-1"),
                new ContextOptimizeRequest.ContextOptimizeMessage("user", "What is the answer?", null, null),
                new ContextOptimizeRequest.ContextOptimizeMessage("assistant", "The answer is 100", null, null),
                new ContextOptimizeRequest.ContextOptimizeMessage("tool", "Remember the answer", "memory", "call-2")
        )));
        assertEquals("summarization", summarization.strategy());
        assertTrue(summarization.changed());
        assertTrue(summarization.projectedMessages().stream().anyMatch(message -> "system".equals(message.role())
                && message.content() != null
                && message.content().startsWith("Context summary: ")));
    }
}
