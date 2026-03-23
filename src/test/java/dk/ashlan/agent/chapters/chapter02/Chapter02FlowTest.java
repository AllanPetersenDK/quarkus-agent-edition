package dk.ashlan.agent.chapters.chapter02;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter02FlowTest {
    @Test
    void llmRequestAndCompletionFlowWorks() {
        assertTrue(Chapter02Support.requestMessages("25 * 4").size() >= 2);
        assertTrue(Chapter02Support.orchestrator().run("25 * 4").finalAnswer().contains("100"));
        assertTrue(Chapter02Support.orchestrator().run("what is the time?").finalAnswer().contains("Current time"));
        assertEquals("potato validated", new PotatoProblemDemo().solve("invalid"));
    }
}
