package dk.ashlan.agent.chapters.chapter05;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter05FlowTest {
    @Test
    void retrievalReturnsRelevantChunk() {
        String answer = Chapter05Support.ingestAndAnswer("arithmetic expressions");

        assertTrue(answer.contains("calculator tool evaluates arithmetic expressions."));
        assertFalse(answer.contains("RAG lets an agent retrieve relevant knowledge before answering."));
    }
}
