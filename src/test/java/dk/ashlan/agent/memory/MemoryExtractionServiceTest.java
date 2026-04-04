package dk.ashlan.agent.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryExtractionServiceTest {
    @Test
    void structuredExtractionProducesProblemSolvingFields() {
        MemoryExtractionService service = new MemoryExtractionService();

        MemoryExtractionResult result = service.extract(
                "session-1",
                "goal",
                "Remember that my favorite database is PostgreSQL."
        );

        assertEquals(MemoryWriteDecision.ADD, result.decision());
        assertNotNull(result.memory());
        assertTrue(result.memory().problem().contains("remember"));
        assertTrue(result.memory().approach().contains("explicit remember"));
        assertTrue(result.memory().result().contains("PostgreSQL"));
    }

    @Test
    void afterRunSignalsKeepCompactAnswerFields() {
        MemoryExtractionService service = new MemoryExtractionService();

        MemoryExtractionResult result = service.extract(
                "session-1",
                "goal",
                "The result is 100 => The result is 100 | trace: iteration:1"
        );

        assertEquals(MemoryWriteDecision.ADD, result.decision());
        assertNotNull(result.memory());
        assertTrue(result.memory().problem().contains("answer"));
        assertEquals("100", result.memory().result());
    }

    @Test
    void rememberSignalsStayCompactAndStripTraceNoise() {
        MemoryExtractionService service = new MemoryExtractionService();

        MemoryExtractionResult result = service.extract(
                "session-1",
                "Remember that I prefer concise answers and PostgreSQL.",
                "Remember that I prefer concise answers and PostgreSQL. => Got it! I will keep answers concise and focus on PostgreSQL when relevant. | trace: iteration:1"
        );

        assertEquals(MemoryWriteDecision.ADD, result.decision());
        assertNotNull(result.memory());
        assertTrue(result.memory().task().contains("Remember that I prefer concise answers and PostgreSQL."));
        assertTrue(result.memory().approach().contains("explicit remember"));
        assertTrue(result.memory().result().contains("Got it!"));
        assertTrue(result.memory().memory().contains("User memory signal:"));
    }
}
