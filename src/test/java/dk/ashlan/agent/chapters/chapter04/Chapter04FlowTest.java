package dk.ashlan.agent.chapters.chapter04;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter04FlowTest {
    @Test
    void callbackAndStructuredOutputFlowsWork() {
        var callbackOrchestrator = Chapter04Support.callbackOrchestrator();
        var result = callbackOrchestrator.run("Solve 25 * 4");
        assertTrue(result.finalAnswer().contains("100"));
        assertTrue(callbackOrchestrator.callbacks().get(0).startsWith("before:"));

        var structuredOutput = Chapter04Support.structuredOutputOrchestrator();
        assertTrue(structuredOutput.normalize("answer: hello structured output").contains("hello structured output"));
    }
}
